package software.amazon.emr.walworkspace;


import software.amazon.awssdk.services.emrwal.EmrwalClient;
import software.amazon.awssdk.services.emrwal.model.DeleteWorkspaceRequest;
import software.amazon.awssdk.services.emrwal.model.EmrwalException;
import software.amazon.awssdk.services.emrwal.model.InvalidResourceException;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import org.apache.commons.lang3.StringUtils;

public class DeleteHandler extends BaseHandlerStd {
    private static final String OPERATION = "AWS-EMR-WALWorkspace::Delete";

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<EmrwalClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        ResourceModel model = request.getDesiredResourceState();
        final String walWorkspaceName = model.getWALWorkspaceName();
        final DeleteWorkspaceRequest deleteWorkspaceRequest = Translator.translateToDeleteRequest(model);

        if (StringUtils.isEmpty(walWorkspaceName)) {
            return ProgressEvent.failed(model,callbackContext, HandlerErrorCode.NotFound, "WalWorkspace was not provided");
        }

        try {
            // Since our Deletion API is a synchronous call, we do not need to apply stabilization check if no exception
            // being thrown
            proxyClient.injectCredentialsAndInvokeV2(deleteWorkspaceRequest, proxyClient.client()::deleteWorkspace);
            logger.log(String.format("Resource %s - [%s] has successfully been deleted.", ResourceModel.TYPE_NAME, walWorkspaceName));
            return ProgressEvent.defaultSuccessHandler(null);
        }
        catch (InvalidResourceException ir) {
            if (ir.getMessage().contains("does not exist")) {
                logger.log(String.format("Resource %s - [%s] is not existed", ResourceModel.TYPE_NAME, walWorkspaceName));
                //A delete handler MUST return FAILED with a NotFound error code if the resource didn't exist before the delete request
                return ProgressEvent.defaultFailureHandler(new CfnGeneralServiceException(OPERATION, ir), HandlerErrorCode.NotFound);
            }
            throw new CfnInvalidRequestException(request.toString());
        }
        catch (Exception exception) {
            logger.log(String.format("Failed to delete workspace: %s for request %s", exception.getMessage(), deleteWorkspaceRequest.toString()));
            return handleError(OPERATION, exception, proxyClient, model, callbackContext);
        }
    }
}
