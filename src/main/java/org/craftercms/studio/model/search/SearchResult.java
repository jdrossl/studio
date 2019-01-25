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

package org.craftercms.studio.model.search;

import java.util.List;

/**
 * Holds all the data for a search operation
 * @author joseross
 */
public class SearchResult {

    /**
     * The total files that matched the search
     */
    protected long total;

    /**
     * The list of files
     */
    protected List<SearchItem> items;

    /**
     * The facets of the matched files
     */
    protected List<SearchFacet> facets;

    public long getTotal() {
        return total;
    }

    public void setTotal(final long total) {
        this.total = total;
    }

    public List<SearchItem> getItems() {
        return items;
    }

    public void setItems(final List<SearchItem> items) {
        this.items = items;
    }

    public List<SearchFacet> getFacets() {
        return facets;
    }

    public void setFacets(final List<SearchFacet> facets) {
        this.facets = facets;
    }

}