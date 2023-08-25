package software.amazon.emr.walworkspace;

import software.amazon.awssdk.services.emrwal.EmrwalClient;
import software.amazon.awssdk.services.emrwal.model.ListWorkspacesRequest;
import software.amazon.awssdk.services.emrwal.model.ListWorkspacesResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ListHandler extends BaseHandlerStd {

    private static final String OPERATION = "AWS-EMR-WALWorkspace::List";
    // Referring to the model the upper limit for list operation is 1000
    // https://code.amazon.com/packages/EMRWALServiceModel/blobs/53562b51351d06abcc6d8f4930a59b3936a7ee32/--/model/main.xml#L299
    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<EmrwalClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        ResourceModel model = request.getDesiredResourceState();
        final List<ResourceModel> models = new ArrayList<>();
        String nextToken = null;

        do {
            final ListWorkspacesResponse listWorkspacesResponse;
            final ListWorkspacesRequest listWorkspacesRequest = Translator.translateToListRequest(MAX_RESULT, request.getNextToken());
            try {
                listWorkspacesResponse = proxyClient.injectCredentialsAndInvokeV2(listWorkspacesRequest, proxyClient.client()::listWorkspaces);
            } catch (Exception exception) {
                return handleError(OPERATION, exception, proxyClient, model, callbackContext);
            }

            if (listWorkspacesResponse != null && listWorkspacesResponse.hasWalWorkspaceList()) {
                models.addAll(Translator.translateFromListResponse(listWorkspacesResponse));
                nextToken = listWorkspacesResponse.nextToken();
            }
        } while (nextToken != null);
        logger.log("Following Resource" + models.toString() + "has successfully been created.");

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .resourceModels(models)
            .status(OperationStatus.SUCCESS)
            .build();
    }
}
