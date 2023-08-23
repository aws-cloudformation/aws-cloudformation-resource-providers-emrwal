package software.amazon.emr.walworkspace;


import software.amazon.awssdk.services.emrwal.EmrwalClient;
import software.amazon.awssdk.services.emrwal.model.InvalidResourceException;
import software.amazon.awssdk.services.emrwal.model.ListTagsForResourceResponse;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandlerStd {
    public static String OPERATION = "AWS-EMR-WALWorkspace::Read";

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<EmrwalClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        logger.log(String.format("[INFO] Read handler request: %s", request));

        ResourceModel model = request.getDesiredResourceState();
        String walWorkspaceName = model.getWALWorkspaceName();

        String accountId = request.getAwsAccountId();
        String partition = request.getAwsPartition();
        String region = request.getRegion();

        //arn:aws:emrwal:us-east-1:759198610994:workspace/dongawstest
        String arn = "arn:" + partition + ":emrwal:" + region + ":" + accountId + ":workspace/" + walWorkspaceName;


        try {
            ListTagsForResourceResponse response = proxyClient.injectCredentialsAndInvokeV2(Translator.translateToReadRequest(arn),
                proxyClient.client()::listTagsForResource);
            model = Translator.translateFromReadResponse(response, walWorkspaceName);
            logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
            return ProgressEvent.defaultSuccessHandler(model);
        } catch(InvalidResourceException ir) {
            logger.log(String.format("% s Resource %s name is incorrect. Here is the stack trace %s ", ResourceModel.TYPE_NAME, arn, ir.getMessage()));
            throw new CfnInvalidRequestException(request.toString());
        } catch (Exception exception) {
            System.out.println("got exception during read:" + exception.getMessage());
            return handleError(OPERATION, exception, proxyClient, model, callbackContext);
        }

    }
}
