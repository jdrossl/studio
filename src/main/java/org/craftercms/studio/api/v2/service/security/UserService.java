/*
 * Copyright (C) 2007-2018 Crafter Software Corporation. All rights reserved.
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
 *
 */

package org.craftercms.studio.api.v2.service.security;

import org.craftercms.studio.api.v1.exception.ServiceLayerException;
import org.craftercms.studio.api.v1.exception.security.AuthenticationException;
import org.craftercms.studio.api.v1.exception.security.UserAlreadyExistsException;
import org.craftercms.studio.api.v1.exception.security.UserNotFoundException;
import org.craftercms.studio.model.*;

import java.util.List;

public interface UserService {

    List<User> getAllUsersForSite(long orgId, String site, int offset, int limit,
                                  String sort) throws ServiceLayerException;

    List<User> getAllUsers(int offset, int limit, String sort) throws ServiceLayerException;

    int getAllUsersForSiteTotal(long orgId, String site) throws ServiceLayerException;

    int getAllUsersTotal() throws ServiceLayerException;

    User createUser(User user) throws UserAlreadyExistsException, ServiceLayerException;

    void updateUser(User user) throws ServiceLayerException;

    void deleteUsers(List<Long> userIds, List<String> usernames) throws ServiceLayerException;

    User getUserByIdOrUsername(long userId, String username) throws ServiceLayerException, UserNotFoundException;

    List<User> enableUsers(List<Long> userIds, List<String> usernames, boolean enabled) throws ServiceLayerException,
                                                                                               UserNotFoundException;

    List<Site> getUserSites(long userId, String username) throws ServiceLayerException, UserNotFoundException;

    List<String> getUserSiteRoles(long userId, String username, String site) throws ServiceLayerException,
                                                                                    UserNotFoundException;

    AuthenticatedUser getCurrentUser() throws AuthenticationException, ServiceLayerException;

    List<Site> getCurrentUserSites() throws AuthenticationException, ServiceLayerException;

    List<String> getCurrentUserSiteRoles(String site) throws AuthenticationException, ServiceLayerException;

    LogoutUrl getCurrentUserLogoutUrl() throws AuthenticationException, ServiceLayerException;

    /*
     TODO: All methods below here should be part of the internal service.
     */

    List<Group> getUserGroups(long userId, String username) throws ServiceLayerException;

    boolean isUserMemberOfGroup(String username, String groupName) throws ServiceLayerException;

}