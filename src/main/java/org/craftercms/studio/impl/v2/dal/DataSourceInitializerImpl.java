/*
 * Copyright (C) 2007-2019 Crafter Software Corporation. All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.craftercms.studio.impl.v2.dal;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ch.vorburger.mariadb4j.springframework.MariaDB4jSpringService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.jdbc.RuntimeSqlException;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.craftercms.commons.crypto.CryptoUtils;
import org.craftercms.commons.entitlements.validator.DbIntegrityValidator;
import org.craftercms.studio.api.v1.log.Logger;
import org.craftercms.studio.api.v1.log.LoggerFactory;
import org.craftercms.studio.api.v1.util.StudioConfiguration;
import org.craftercms.studio.api.v2.dal.DataSourceInitializer;

import static org.craftercms.studio.api.v1.util.StudioConfiguration.CLUSTERING_NODE_REGISTRATION;
import static org.craftercms.studio.api.v1.util.StudioConfiguration.DB_DRIVER;
import static org.craftercms.studio.api.v1.util.StudioConfiguration.DB_INITIALIZER_CONFIGURE_DB_SCRIPT_LOCATION;
import static org.craftercms.studio.api.v1.util.StudioConfiguration.DB_INITIALIZER_CREATE_DB_SCRIPT_LOCATION;
import static org.craftercms.studio.api.v1.util.StudioConfiguration.DB_INITIALIZER_CREATE_SCHEMA_SCRIPT_LOCATION;
import static org.craftercms.studio.api.v1.util.StudioConfiguration.DB_INITIALIZER_ENABLED;
import static org.craftercms.studio.api.v1.util.StudioConfiguration.DB_INITIALIZER_RANDOM_ADMIN_PASSWORD_CHARS;
import static org.craftercms.studio.api.v1.util.StudioConfiguration.DB_INITIALIZER_RANDOM_ADMIN_PASSWORD_ENABLED;
import static org.craftercms.studio.api.v1.util.StudioConfiguration.DB_INITIALIZER_RANDOM_ADMIN_PASSWORD_LENGTH;
import static org.craftercms.studio.api.v1.util.StudioConfiguration.DB_INITIALIZER_URL;

public class DataSourceInitializerImpl implements DataSourceInitializer {

    private final static Logger logger = LoggerFactory.getLogger(DataSourceInitializerImpl.class);

    /**
     * Database queries
     */
    private final static String DB_QUERY_CHECK_CONFIG =
            "select @@GLOBAL.innodb_large_prefix, @@GLOBAL.innodb_file_format, @@GLOBAL.innodb_file_format_max, " +
                    "@@GLOBAL.innodb_file_per_table";
    private final static String DB_QUERY_CHECK_SCHEMA_EXISTS =
            "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = 'crafter'";
    private final static String DB_QUERY_CHECK_TABLES = "SHOW TABLES FROM crafter";
    private final static String DB_QUERY_SET_ADMIN_PASSWORD =
            "UPDATE user SET password = '{password}' WHERE username = 'admin'";

    protected String delimiter;
    protected StudioConfiguration studioConfiguration;

    protected DbIntegrityValidator integrityValidator;

    protected MariaDB4jSpringService embeddedService;

    @Override
    public void initDataSource() {

        // Stop embedded service in clustered environment
        if (studioConfiguration.getProperty(CLUSTERING_NODE_REGISTRATION, new HashMap<String, String>().getClass()) != null) {
            embeddedService.stop();
        }

        if (isEnabled()) {
            String configureDbScriptPath = getConfigureDBScriptPath();

            try {
                Class.forName(studioConfiguration.getProperty(DB_DRIVER));
            } catch (Exception e) {
                logger.error("Error connecting to database", e);
            }

            try(Connection conn = DriverManager.getConnection(studioConfiguration.getProperty(DB_INITIALIZER_URL))) {
                // Configure DB
                logger.debug("Check if database is already configured properly");
                boolean dbConfigured = false;
                try (Statement statement = conn.createStatement();
                    ResultSet rs = statement.executeQuery(DB_QUERY_CHECK_CONFIG)) {
                    if (rs.next()) {
                        int largePrefix = rs.getInt(1);
                        String fileFormat = rs.getString(2);
                        String fileFormatMax = rs.getString(3);
                        int filePerTable = rs.getInt(4);
                        dbConfigured =
                                (largePrefix == 1) && StringUtils.equalsIgnoreCase("BARRACUDA", fileFormat) &&
                                        StringUtils.equalsIgnoreCase("BARRACUDA", fileFormatMax) && (filePerTable == 1);
                    }
                }

                ScriptRunner sr = new ScriptRunner(conn);
                InputStream is = null;
                Reader reader = null;
                if (!dbConfigured) {
                    logger.info("Configure database from script " + configureDbScriptPath);
                    sr.setDelimiter(delimiter);
                    sr.setStopOnError(true);
                    sr.setLogWriter(null);
                    is = getClass().getClassLoader().getResourceAsStream(configureDbScriptPath);
                    reader = new InputStreamReader(is);
                    try {
                        sr.runScript(reader);
                    } catch (RuntimeSqlException e) {
                        logger.error("Error while running configure DB script", e);
                    }
                }

                logger.debug("Check if database schema already exists");
                try(Statement statement = conn.createStatement();
                    ResultSet rs = statement.executeQuery(DB_QUERY_CHECK_SCHEMA_EXISTS)) {

                    if (rs.next()) {
                        logger.debug("Database schema exists. Check if it is empty.");
                        try (ResultSet rs2 = statement.executeQuery(DB_QUERY_CHECK_TABLES)) {
                            List<String> tableNames = new ArrayList<String>();
                            while (rs2.next()) {
                                tableNames.add(rs2.getString(1));
                            }
                            if (tableNames.size() == 0) {
                                createDatabaseTables(conn, statement);
                            } else {
                                logger.debug("Database already exists. Validate the integrity of the database");
                            }
                        }
                    } else {
                        // Database does not exist
                        createSchema(conn);
                        createDatabaseTables(conn, statement);
                    }
                } catch (SQLException e) {
                    logger.error("Error while initializing database", e);
                }
            } catch (SQLException e) {
                if (logger.getLevel().equals(Logger.LEVEL_DEBUG)) {
                    logger.error("Error while connecting to initialize DB", e);
                } else {
                    logger.error("Error while connecting to initialize DB");
                }

            }
        }
    }

    private void createDatabaseTables(Connection conn, Statement statement) throws SQLException {
        String createDbScriptPath = getCreateDBScriptPath();
        // Database does not exist
        logger.info("Database tables do not exist.");
        logger.info("Creating database tables from script " + createDbScriptPath);
        ScriptRunner sr = new ScriptRunner(conn);

        sr.setDelimiter(delimiter);
        sr.setStopOnError(true);
        sr.setLogWriter(null);
        InputStream is = getClass().getClassLoader().getResourceAsStream(createDbScriptPath);
        Reader reader = new InputStreamReader(is);
        try {
            sr.runScript(reader);

            if (isRandomAdminPasswordEnabled()) {
                String randomPassword = generateRandomPassword();
                String hashedPassword = CryptoUtils.hashPassword(randomPassword);
                String update = DB_QUERY_SET_ADMIN_PASSWORD.replace("{password}", hashedPassword);
                statement.executeUpdate(update);
                conn.commit();
                logger.info("*** Admin Account Password: \"" + randomPassword + "\" ***");
            }

            integrityValidator.store(conn);
        } catch (RuntimeSqlException e) {
            logger.error("Error while running create DB script", e);
        }
    }

    private void createSchema(Connection conn)  {
        String createSchemaScriptPath = getCreateSchemaScriptPath();
        // Database does not exist
        logger.info("Database schema does not exists.");
        logger.info("Creating database schema from script " + createSchemaScriptPath);
        ScriptRunner sr = new ScriptRunner(conn);

        sr.setDelimiter(delimiter);
        sr.setStopOnError(true);
        sr.setLogWriter(null);
        InputStream is = getClass().getClassLoader().getResourceAsStream(createSchemaScriptPath);
        Reader reader = new InputStreamReader(is);
        try {
            sr.runScript(reader);
        } catch (RuntimeSqlException e) {
            logger.error("Error while running create DB script", e);
        }
    }

    public boolean isEnabled() {
        boolean toReturn = Boolean.parseBoolean(studioConfiguration.getProperty(DB_INITIALIZER_ENABLED));
        return toReturn;
    }

    private String generateRandomPassword() {
        int passwordLength = Integer.parseInt(
                studioConfiguration.getProperty(DB_INITIALIZER_RANDOM_ADMIN_PASSWORD_LENGTH));
        String passwordChars = studioConfiguration.getProperty(DB_INITIALIZER_RANDOM_ADMIN_PASSWORD_CHARS);
        return RandomStringUtils.random(passwordLength, passwordChars);
    }

    private String getConfigureDBScriptPath() {
        return studioConfiguration.getProperty(DB_INITIALIZER_CONFIGURE_DB_SCRIPT_LOCATION);
    }

    private String getCreateDBScriptPath() {
        return studioConfiguration.getProperty(DB_INITIALIZER_CREATE_DB_SCRIPT_LOCATION);
    }

    private String getCreateSchemaScriptPath() {
        return studioConfiguration.getProperty(DB_INITIALIZER_CREATE_SCHEMA_SCRIPT_LOCATION);
    }

    private boolean isRandomAdminPasswordEnabled() {
        boolean toRet = Boolean.parseBoolean(
                studioConfiguration.getProperty(DB_INITIALIZER_RANDOM_ADMIN_PASSWORD_ENABLED));
        return toRet;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public void setStudioConfiguration(StudioConfiguration studioConfiguration) {
        this.studioConfiguration = studioConfiguration;
    }

    public void setIntegrityValidator(final DbIntegrityValidator integrityValidator) {
        this.integrityValidator = integrityValidator;
    }

    public MariaDB4jSpringService getEmbeddedService() {
        return embeddedService;
    }

    public void setEmbeddedService(MariaDB4jSpringService embeddedService) {
        this.embeddedService = embeddedService;
    }
}
