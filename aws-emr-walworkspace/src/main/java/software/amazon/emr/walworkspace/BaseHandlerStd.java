package software.amazon.emr.walworkspace;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.services.emrwal.EmrwalClient;
import software.amazon.awssdk.services.emrwal.model.EmrwalException;
import software.amazon.awssdk.services.emrwal.model.ListWorkspacesRequest;
import software.amazon.awssdk.services.emrwal.model.ListWorkspacesResponse;
import software.amazon.awssdk.services.emrwal.model.TaggingFailedException;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.awssdk.services.emrwal.model.WalThrottlingException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

// Base class for functionality that could be shared across Create/Read/Delete/List Handlers

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {
    static final Set<Class<? extends Exception>> RETRYABLE_EXCEPTIONS =
        ImmutableSet.<Class<? extends Exception>>builder().add(TaggingFailedException.class)
            .add(WalThrottlingException.class)
            .build();

    static final int MAX_RESULT = 1000;

    Logger logger;
    @Override
    public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
        final Logger logger) {
        return handleRequest(
            proxy,
            request,
            callbackContext != null ? callbackContext : new CallbackContext(),
            proxy.newProxy(ClientBuilder::getClient),
            logger);
    }

    protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
        final ProxyClient<EmrwalClient> proxyClient, final Logger logger);

    /***
     * Call ListWalWorkspace API and evaluate whether the workspace is there or not.
     *
     * Return true if the workspace exists, otherwise false.
     *
     * @param walWorkspaceName
     * @param proxyClient
     * @return boolean
     *
     * Todo:  We might need a dedicate API for check wal workspace status API later
     *
     * For now, we have a meta layer build on top of DynamoDB, it is quite fast to retrieve whether a workspace is
     * in the return result list or not.
     */
    protected boolean readResource(final ProxyClient<EmrwalClient> proxyClient, final String walWorkspaceName) {
        ListWorkspacesResponse response;

        List<String> walWorkspaceList = new ArrayList<>();
        final ListWorkspacesRequest listWorkspacesRequest = ListWorkspacesRequest.builder().maxResults(10000).build();

        walWorkspaceList = proxyClient.injectCredentialsAndInvokeV2(listWorkspacesRequest, proxyClient.client()::listWorkspaces).walWorkspaceList();

        if(walWorkspaceList.contains(walWorkspaceName))
                return true;

        return false;
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleError(
        final String operation,
        final Exception exception,
        final ProxyClient<EmrwalClient> emrwalClientProxyClient,
        final ResourceModel model,
        final CallbackContext callbackContext) {
        logger.log(String.format("[ERROR] handleError for %s, error: %s", operation, exception));


        BaseHandlerException ex = (exception instanceof EmrwalException) ?
            Translator.translate((EmrwalException) exception, operation, model.getWALWorkspaceName(),
                callbackContext) :
            new CfnGeneralServiceException(operation, exception);

        //If resource already exists, return FAILED with AlreadyExist error code
        if (operation.equals(CreateHandler.OPERATION)){
            if (exception.getMessage() != null && exception.getMessage().contains("already exists")){
                return ProgressEvent.defaultFailureHandler(ex, HandlerErrorCode.AlreadyExists);
            }
        }

        //An update handler MUST return FAILED with a NotFound error code if the resource didn't exist
        // before the update request.
        if (operation.equals(UpdateHandler.OPERATION) && exception instanceof TaggingFailedException){
            if (exception.getMessage() != null &&
                    exception.getMessage().contains("Unable to retrieve workspace for tagging")){
                return ProgressEvent.defaultFailureHandler(ex, HandlerErrorCode.NotFound);
            }
        }

        //For the read, we call listTagsForResource to confirm if the workspace there or not
        //so if workspace not there, it will throw:"Unable to list tag, please make sure Workspace is created first"
        if ((operation.equals(ReadHandler.OPERATION) || operation.equals(UpdateHandler.OPERATION)) && exception instanceof TaggingFailedException){
            if (exception.getMessage() != null &&
                    exception.getMessage().contains("Unable to list tag, please make sure Workspace is created first")){
                return ProgressEvent.defaultFailureHandler(ex, HandlerErrorCode.NotFound);
            }
        }

        if (isRetryableException(exception) && callbackContext.getRetryAttempts() > 0) {
            // this will allow failed operation to retry
            callbackContext.retryAttempts = callbackContext.getRetryAttempts() - 1;
            throw RetryableException.create(exception.getMessage(), exception);
        }

        return ProgressEvent.defaultFailureHandler(ex, HandlerErrorCode.GeneralServiceException);
    }

    protected boolean isRetryableException(final Exception e) {
        return Optional.ofNullable(e).map(Exception::getClass).map(RETRYABLE_EXCEPTIONS::contains)
            .orElse(false);
    }
}
