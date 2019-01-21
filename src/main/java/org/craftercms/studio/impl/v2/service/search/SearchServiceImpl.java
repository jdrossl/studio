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

package org.craftercms.studio.impl.v2.service.search;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.craftercms.studio.api.v1.exception.security.AuthenticationException;
import org.craftercms.studio.api.v1.service.security.SecurityService;
import org.craftercms.studio.api.v2.service.search.SearchService;
import org.craftercms.studio.api.v2.service.search.internal.SearchServiceInternal;
import org.craftercms.studio.model.search.SearchParams;
import org.craftercms.studio.model.search.SearchResult;
import org.springframework.beans.factory.annotation.Required;

/**
 * @author joseross
 */
public class SearchServiceImpl implements SearchService {

    protected SecurityService securityService;
    protected SearchServiceInternal searchServiceInternal;

    @Required
    public void setSecurityService(final SecurityService securityService) {
        this.securityService = securityService;
    }

    @Required
    public void setSearchServiceInternal(final SearchServiceInternal searchServiceInternal) {
        this.searchServiceInternal = searchServiceInternal;
    }

    @Override
    public SearchResult search(final String siteId, final SearchParams params)
        throws AuthenticationException, IOException {
        String user = securityService.getCurrentUser();
        if(StringUtils.isNotEmpty(user)) {
            // TODO: Get allowed paths from the security service
            List<String> allowedPaths = Collections.emptyList();
            return searchServiceInternal.search(siteId, allowedPaths, params);
        } else {
            throw new AuthenticationException("User is not authenticated");
        }
    }

}
