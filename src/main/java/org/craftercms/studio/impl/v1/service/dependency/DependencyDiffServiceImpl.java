/*
 * Crafter Studio Web-content authoring solution
 * Copyright (C) 2007-2017 Crafter Software Corporation.
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

package org.craftercms.studio.impl.v1.service.dependency;

import org.apache.commons.lang.StringUtils;
import org.craftercms.studio.api.v1.exception.ServiceException;
import org.craftercms.studio.api.v1.service.dependency.DependencyDiffService;
import org.craftercms.studio.api.v1.service.dependency.DmDependencyService;

import java.util.ArrayList;
import java.util.List;

public class DependencyDiffServiceImpl implements DependencyDiffService {

    protected DmDependencyService dependencyService;

    public DmDependencyService getDependencyService() { return dependencyService; }
    public void setDependencyService(DmDependencyService dependencyService) { this.dependencyService = dependencyService; }

    /**
     * Computes addedDependenices and removedDependenices based on the DiffRequest information provided
     * @param diffRequest
     * @return diff response object
     * @throws ServiceException
     */
    public DiffResponse diff(DiffRequest diffRequest) throws ServiceException {

        if(diffRequest == null)
            throw new ServiceException("diffcontext cannot be null");

        DiffResponse response = new DiffResponse();
        boolean recursive = diffRequest.isRecursive();
        String site = diffRequest.getSite();

        String sourcePath = diffRequest.getSourcePath();
        String destPath = diffRequest.getDestPath();
        if(StringUtils.isEmpty(destPath)){
            destPath = sourcePath;
        }

        List<String> sourceDependencies = new ArrayList<String>();
        sourceDependencies = findDependencies(site,diffRequest.getSourceSandbox(),sourcePath, recursive, sourceDependencies);
        List<String> destDependencies =  new ArrayList<String>();
        destDependencies = findDependencies(site,diffRequest.getDestSandbox(),destPath, recursive, destDependencies);

        //Removed dependenices
        for(String destDependency:destDependencies){
            if(!sourceDependencies.contains(destDependency)){
                response.getRemovedDependencies().add(destDependency);
            }
        }
        //Added dependenices
        for(String sourceDependency:sourceDependencies){
            if(!destDependencies.contains(sourceDependency)){
                response.getAddedDependencies().add(sourceDependency);
            }
        }
        return response;
    }

    protected List<String> findDependencies(String site, String sandbox, String relativePath, boolean isRecursive, List<String> dependencies) throws ServiceException{
        List<String> dependenciesFromDoc = dependencyService.getDependencyPaths(site, relativePath);
        dependencies.addAll(dependenciesFromDoc);
        if(isRecursive){
            for(String dependency:dependenciesFromDoc){
                if (!dependencies.contains(dependency)) {
                    dependencies.addAll(findDependencies(site, sandbox, dependency, isRecursive, dependencies));
                }
            }
        }
        return dependencies;
    }
}
