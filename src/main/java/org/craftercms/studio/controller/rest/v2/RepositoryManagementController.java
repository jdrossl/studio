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

package org.craftercms.studio.controller.rest.v2;

import org.craftercms.commons.crypto.CryptoException;
import org.craftercms.studio.api.v1.exception.ServiceLayerException;
import org.craftercms.studio.api.v1.exception.SiteNotFoundException;
import org.craftercms.studio.api.v1.exception.repository.InvalidRemoteUrlException;
import org.craftercms.studio.api.v1.service.site.SiteService;
import org.craftercms.studio.api.v2.dal.DiffConflictedFile;
import org.craftercms.studio.api.v2.dal.RemoteRepository;
import org.craftercms.studio.api.v2.dal.RemoteRepositoryInfo;
import org.craftercms.studio.api.v2.dal.RepositoryStatus;
import org.craftercms.studio.api.v2.exception.PullFromRemoteConflictException;
import org.craftercms.studio.api.v2.service.repository.RepositoryManagementService;
import org.craftercms.studio.model.rest.CancelFailedPullRequest;
import org.craftercms.studio.model.rest.CommitResolutionRequest;
import org.craftercms.studio.model.rest.PullFromRemoteRequest;
import org.craftercms.studio.model.rest.PushToRemoteRequest;
import org.craftercms.studio.model.rest.RebuildDatabaseRequest;
import org.craftercms.studio.model.rest.RemoveRemoteRequest;
import org.craftercms.studio.model.rest.ResolveConflictRequest;
import org.craftercms.studio.model.rest.ResponseBody;
import org.craftercms.studio.model.rest.Result;
import org.craftercms.studio.model.rest.ResultList;
import org.craftercms.studio.model.rest.ResultOne;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.craftercms.studio.api.v1.constant.StudioConstants.FILE_SEPARATOR;
import static org.craftercms.studio.controller.rest.v2.RequestConstants.REQUEST_PARAM_PATH;
import static org.craftercms.studio.controller.rest.v2.RequestConstants.REQUEST_PARAM_SITEID;
import static org.craftercms.studio.controller.rest.v2.ResultConstants.RESULT_KEY_DIFF;
import static org.craftercms.studio.controller.rest.v2.ResultConstants.RESULT_KEY_REMOTES;
import static org.craftercms.studio.controller.rest.v2.ResultConstants.RESULT_KEY_REPOSITORY_STATUS;
import static org.craftercms.studio.model.rest.ApiResponse.CREATED;
import static org.craftercms.studio.model.rest.ApiResponse.INTERNAL_SYSTEM_FAILURE;
import static org.craftercms.studio.model.rest.ApiResponse.OK;

@RestController
@RequestMapping("/api/2/repository")
public class RepositoryManagementController {

    private RepositoryManagementService repositoryManagementService;
    private SiteService siteService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/add_remote")
    public ResponseBody addRemote(@RequestBody RemoteRepository remoteRepository)
            throws ServiceLayerException, InvalidRemoteUrlException {

        if (!siteService.exists(remoteRepository.getSiteId())) {
            throw new SiteNotFoundException(remoteRepository.getSiteId());
        }

        boolean res = repositoryManagementService.addRemote(remoteRepository.getSiteId(), remoteRepository);

        ResponseBody responseBody = new ResponseBody();
        Result result = new Result();
        if (res) {
            result.setResponse(CREATED);
        } else {
            result.setResponse(INTERNAL_SYSTEM_FAILURE);
        }
        responseBody.setResult(result);
        return responseBody;
    }

    @GetMapping("/list_remotes")
    public ResponseBody listRemotes(@RequestParam(name = "siteId", required = true) String siteId)
            throws ServiceLayerException, CryptoException {
        if (!siteService.exists(siteId)) {
            throw new SiteNotFoundException(siteId);
        }
        List<RemoteRepositoryInfo> remotes = repositoryManagementService.listRemotes(siteId);

        ResponseBody responseBody = new ResponseBody();
        ResultList<RemoteRepositoryInfo> result = new ResultList<RemoteRepositoryInfo>();
        result.setEntities(RESULT_KEY_REMOTES, remotes);
        result.setResponse(OK);
        responseBody.setResult(result);
        return responseBody;
    }

    @PostMapping("/pull_from_remote")
    public ResponseBody pullFromRemote(@RequestBody PullFromRemoteRequest pullFromRemoteRequest)
            throws InvalidRemoteUrlException, ServiceLayerException, CryptoException {
        if (!siteService.exists(pullFromRemoteRequest.getSiteId())) {
            throw new SiteNotFoundException(pullFromRemoteRequest.getSiteId());
        }
        boolean res = repositoryManagementService.pullFromRemote(pullFromRemoteRequest.getSiteId(),
                pullFromRemoteRequest.getRemoteName(), pullFromRemoteRequest.getRemoteBranch(),
                pullFromRemoteRequest.getMergeStrategy());

        if (!res) {
            throw new PullFromRemoteConflictException("Pull from remote result is merge conflict.");
        }

        ResponseBody responseBody = new ResponseBody();
        Result result = new Result();
        result.setResponse(OK);
        responseBody.setResult(result);
        return responseBody;
    }

    @PostMapping("/push_to_remote")
    public ResponseBody pushToRemote(@RequestBody PushToRemoteRequest pushToRemoteRequest)
            throws InvalidRemoteUrlException, CryptoException, ServiceLayerException {
        if (!siteService.exists(pushToRemoteRequest.getSiteId())) {
            throw new SiteNotFoundException(pushToRemoteRequest.getSiteId());
        }
        boolean res = repositoryManagementService.pushToRemote(pushToRemoteRequest.getSiteId(),
                pushToRemoteRequest.getRemoteName(), pushToRemoteRequest.getRemoteBranch(),
                pushToRemoteRequest.isForce());

        ResponseBody responseBody = new ResponseBody();
        Result result = new Result();
        if (res) {
            result.setResponse(OK);
        } else {
            result.setResponse(INTERNAL_SYSTEM_FAILURE);
        }
        responseBody.setResult(result);
        return responseBody;
    }

    @PostMapping("/rebuild_database")
    public ResponseBody rebuildDatabase(@RequestBody RebuildDatabaseRequest rebuildDatabaseRequest)
            throws SiteNotFoundException {
        if (!siteService.exists(rebuildDatabaseRequest.getSiteId())) {
            throw new SiteNotFoundException(rebuildDatabaseRequest.getSiteId());
        }
        repositoryManagementService.rebuildDatabase(rebuildDatabaseRequest.getSiteId());

        ResponseBody responseBody = new ResponseBody();
        Result result = new Result();
        result.setResponse(OK);
        responseBody.setResult(result);
        return responseBody;
    }

    @PostMapping("/remove_remote")
    public ResponseBody removeRemote(@RequestBody RemoveRemoteRequest removeRemoteRequest)
            throws CryptoException, SiteNotFoundException {
        if (!siteService.exists(removeRemoteRequest.getSiteId())) {
            throw new SiteNotFoundException(removeRemoteRequest.getSiteId());
        }
        boolean res = repositoryManagementService.removeRemote(removeRemoteRequest.getSiteId(),
                removeRemoteRequest.getRemoteName());

        ResponseBody responseBody = new ResponseBody();
        Result result = new Result();
        if (res) {
            result.setResponse(OK);
        } else {
            result.setResponse(INTERNAL_SYSTEM_FAILURE);
        }
        responseBody.setResult(result);
        return responseBody;
    }

    @GetMapping("/status")
    public ResponseBody getRepositoryStatus(@RequestParam(value = REQUEST_PARAM_SITEID) String siteId)
            throws ServiceLayerException, CryptoException {
        if (!siteService.exists(siteId)) {
            throw new SiteNotFoundException(siteId);
        }
        RepositoryStatus status = repositoryManagementService.getRepositoryStatus(siteId);
        ResponseBody responseBody = new ResponseBody();
        ResultOne<RepositoryStatus> result = new ResultOne<RepositoryStatus>();
        result.setEntity(RESULT_KEY_REPOSITORY_STATUS, status);
        result.setResponse(OK);
        responseBody.setResult(result);
        return responseBody;
    }

    @PostMapping("/resolve_conflict")
    public ResponseBody resolveConflict(@RequestBody ResolveConflictRequest resolveConflictRequest)
            throws ServiceLayerException, CryptoException {
        if (!siteService.exists(resolveConflictRequest.getSiteId())) {
            throw new SiteNotFoundException(resolveConflictRequest.getSiteId());
        }
        String path = resolveConflictRequest.getPath();
        if (!path.startsWith(FILE_SEPARATOR)) {
            path = FILE_SEPARATOR + path;
        }
        RepositoryStatus status = repositoryManagementService.resolveConflict(resolveConflictRequest.getSiteId(),
                path, resolveConflictRequest.getResolution());
        ResponseBody responseBody = new ResponseBody();
        ResultOne<RepositoryStatus> result = new ResultOne<RepositoryStatus>();
        result.setResponse(OK);
        result.setEntity(RESULT_KEY_REPOSITORY_STATUS, status);
        responseBody.setResult(result);
        return responseBody;
    }

    @GetMapping("/diff_conflicted_file")
    public ResponseBody getDiffForConflictedFile(@RequestParam(value = REQUEST_PARAM_SITEID) String siteId,
                                                 @RequestParam(value = REQUEST_PARAM_PATH) String path)
            throws ServiceLayerException, CryptoException {
        if (!siteService.exists(siteId)) {
            throw new SiteNotFoundException(siteId);
        }
        String diffPath = path;
        if (!diffPath.startsWith(FILE_SEPARATOR)) {
            diffPath = FILE_SEPARATOR + diffPath;
        }
        DiffConflictedFile diff = repositoryManagementService.getDiffForConflictedFile(siteId, diffPath);
        ResponseBody responseBody = new ResponseBody();
        ResultOne<DiffConflictedFile> result = new ResultOne<DiffConflictedFile>();
        result.setEntity(RESULT_KEY_DIFF, diff);
        result.setResponse(OK);
        responseBody.setResult(result);
        return  responseBody;
    }

    @PostMapping("/commit_resolution")
    public ResponseBody commitConflictResolution(@RequestBody CommitResolutionRequest commitResolutionRequest)
            throws ServiceLayerException, CryptoException {
        if (!siteService.exists(commitResolutionRequest.getSiteId())) {
            throw new SiteNotFoundException(commitResolutionRequest.getSiteId());
        }
        RepositoryStatus status = repositoryManagementService.commitResolution(commitResolutionRequest.getSiteId(),
                commitResolutionRequest.getCommitMessage());
        ResponseBody responseBody = new ResponseBody();
        ResultOne<RepositoryStatus> result = new ResultOne<RepositoryStatus>();
        result.setEntity(RESULT_KEY_REPOSITORY_STATUS, status);
        result.setResponse(OK);
        responseBody.setResult(result);
        return responseBody;
    }

    @PostMapping("/cancel_failed_pull")
    public ResponseBody cancelFailedPull(@RequestBody CancelFailedPullRequest cancelFailedPullRequest)
            throws ServiceLayerException, CryptoException {
        if (!siteService.exists(cancelFailedPullRequest.getSiteId())) {
            throw new SiteNotFoundException(cancelFailedPullRequest.getSiteId());
        }
        RepositoryStatus status = repositoryManagementService.cancelFailedPull(cancelFailedPullRequest.getSiteId());
        ResponseBody responseBody = new ResponseBody();
        ResultOne<RepositoryStatus> result = new ResultOne<RepositoryStatus>();
        result.setEntity(RESULT_KEY_REPOSITORY_STATUS, status);
        result.setResponse(OK);
        responseBody.setResult(result);
        return responseBody;
    }

    public RepositoryManagementService getRepositoryManagementService() {
        return repositoryManagementService;
    }

    public void setRepositoryManagementService(RepositoryManagementService repositoryManagementService) {
        this.repositoryManagementService = repositoryManagementService;
    }

    public SiteService getSiteService() {
        return siteService;
    }

    public void setSiteService(SiteService siteService) {
        this.siteService = siteService;
    }
}
