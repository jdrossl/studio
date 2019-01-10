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

package org.craftercms.studio.impl.v1.entitlement;

import java.util.Arrays;
import java.util.List;

import org.craftercms.commons.entitlements.model.Entitlement;
import org.craftercms.commons.entitlements.model.EntitlementType;
import org.craftercms.commons.entitlements.model.Module;
import org.craftercms.commons.entitlements.usage.EntitlementUsageProvider;
import org.craftercms.studio.api.v1.exception.ServiceLayerException;
import org.craftercms.studio.api.v1.log.Logger;
import org.craftercms.studio.api.v1.log.LoggerFactory;
import org.craftercms.studio.api.v1.service.content.ObjectMetadataManager;
import org.craftercms.studio.api.v1.service.site.SiteService;
import org.craftercms.studio.api.v2.service.security.internal.UserServiceInternal;

import static org.craftercms.commons.entitlements.model.Module.STUDIO;

/**
 * Implementation of {@link EntitlementUsageProvider} for Crafter Studio module.
 *
 * @author joseross
 */
public class StudioEntitlementUsageProvider implements EntitlementUsageProvider {

    private static final Logger logger = LoggerFactory.getLogger(EntitlementUsageProvider.class);

    /**
     * Current instance of {@link ObjectMetadataManager}.
     */
    protected ObjectMetadataManager objectMetadataManager;

    /**
     * Current instance of {@link SiteService}.
     */
    protected SiteService siteService;

    protected UserServiceInternal userServiceInternal;

    public void setObjectMetadataManager(final ObjectMetadataManager objectMetadataManager) {
        this.objectMetadataManager = objectMetadataManager;
    }

    public void setSiteService(final SiteService siteService) {
        this.siteService = siteService;
    }

    public UserServiceInternal getUserServiceInternal() {
        return userServiceInternal;
    }

    public void setUserServiceInternal(UserServiceInternal userServiceInternal) {
        this.userServiceInternal = userServiceInternal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Module getModule() {
        return STUDIO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Entitlement> getCurrentUsage() {
        Entitlement sites = new Entitlement();
        sites.setType(EntitlementType.SITE);
        sites.setValue(siteService.countSites());

        Entitlement users = new Entitlement();
        users.setType(EntitlementType.USER);
        try {
            users.setValue(userServiceInternal.getAllUsersTotal());
        } catch (ServiceLayerException e) {
            logger.warn("Error getting total users", e);
            users.setValue(0);
        }

        Entitlement items = new Entitlement();
        items.setType(EntitlementType.ITEM);
        items.setValue(objectMetadataManager.countAllItems());

        return Arrays.asList(sites, users, items, items);
    }

}
