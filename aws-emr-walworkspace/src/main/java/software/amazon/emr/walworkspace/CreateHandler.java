package software.amazon.emr.walworkspace;


import software.amazon.awssdk.services.emrwal.EmrwalClient;
import software.amazon.awssdk.services.emrwal.model.CreateWorkspaceRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;


public class CreateHandler extends BaseHandlerStd {

    public static String OPERATION = "AWS-EMR-WALWorkspace::Create";

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<EmrwalClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        ResourceModel model = request.getDesiredResourceState();
        final String walWorkspaceName = model.getWALWorkspaceName();
        final CreateWorkspaceRequest createWorkspaceRequest = Translator.translateToCreateRequest(model);

        try {
            // Since our Creation API is a synchronous call, we do not need to apply stabilization check if no exception
            // being thrown
            proxyClient.injectCredentialsAndInvokeV2(createWorkspaceRequest, proxyClient.client()::createWorkspace);
            logger.log(String.format("Resource %s - [%s] has successfully been created.", ResourceModel.TYPE_NAME, model.getWALWorkspaceName()));
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.SUCCESS)
                .build();
        }
        catch (Exception exception) {
            logger.log(String.format("Failed to create workspace: %s for request %s", exception.getMessage(), createWorkspaceRequest.toString()));
            return handleError(OPERATION, exception, proxyClient, model, callbackContext);
        }
    }
}