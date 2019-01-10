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

package org.craftercms.studio.impl.v1.service;

import org.craftercms.core.service.Context;
import org.craftercms.core.store.ContentStoreAdapter;

public class StudioCacheContext implements Context {

    private final String CONFIG_CONTEXT = "StudioConfiguration";
    private final String CONTENT_CONTEXT = "StudioContent";

    public StudioCacheContext(String site) {
        this(site, false);
    }

    public StudioCacheContext(String site, boolean isConfig) {
        this.isConfig = isConfig;
        this.site = site;
        StringBuilder sb = new StringBuilder();
        if (isConfig) {
            sb.append(CONFIG_CONTEXT);
        } else {
            sb.append(CONTENT_CONTEXT);
        }
        sb.append(":").append(site);
        contextId = sb.toString();
    }

    @Override
    public String getId() {
        return contextId;
    }

    @Override
    public ContentStoreAdapter getStoreAdapter() {
        return null;
    }

    @Override
    public String getStoreServerUrl() {
        return null;
    }

    @Override
    public String getRootFolderPath() {
        return null;
    }

    @Override
    public boolean isMergingOn() {
        return DEFAULT_MERGING_ON;
    }

    @Override
    public boolean isCacheOn() {
        return DEFAULT_CACHE_ON;
    }

    @Override
    public int getMaxAllowedItemsInCache() {
        return DEFAULT_MAX_ALLOWED_ITEMS_IN_CACHE;
    }

    @Override
    public boolean ignoreHiddenFiles() {
        return DEFAULT_IGNORE_HIDDEN_FILES;
    }

    protected boolean isConfig;
    protected String site;
    protected String contextId;

}
