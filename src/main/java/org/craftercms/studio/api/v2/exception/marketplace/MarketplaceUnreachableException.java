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

package org.craftercms.studio.api.v2.exception.marketplace;

/**
 * Exception thrown when the connection with the Marketplace fails
 *
 * @author joseross
 * @since 3.1.4
 */
public class MarketplaceUnreachableException extends MarketplaceException {

    /**
     * Current URL for the Marketplace
     */
    protected String url;

    public MarketplaceUnreachableException(final String url, final Exception e) {
        super("Marketplace is not available at URL: " + url, e);
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

}