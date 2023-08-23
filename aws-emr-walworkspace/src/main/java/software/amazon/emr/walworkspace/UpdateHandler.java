package software.amazon.emr.walworkspace;

import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import software.amazon.awssdk.services.emrwal.EmrwalClient;
import software.amazon.awssdk.services.emrwal.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.emrwal.model.TagResourceRequest;
import software.amazon.awssdk.services.emrwal.model.UntagResourceRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import org.apache.commons.lang3.StringUtils;

public class UpdateHandler extends BaseHandlerStd {
    public static final String OPERATION = "AWS-EMR-WALWorkspace::Update";

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<EmrwalClient> proxyClient,
        final Logger logger
    ) {

        this.logger = logger;

        ResourceModel model = request.getDesiredResourceState();

        // Get cloudformation stack tags from previous stage and current stage
        logger.log("Fetch cloudformation stack tags.");
        Set<Tag> previousTags = Translator.convertResourceTagsToSet(request.getPreviousResourceTags());
        Set<Tag> desiredTags = Translator.convertResourceTagsToSet(request.getDesiredResourceTags());

        // Get system tag from previous stage and current stage
        logger.log("Fetch system tags.");
        previousTags.addAll(Translator.convertResourceTagsToSet(request.getPreviousSystemTags()));
        desiredTags.addAll(Translator.convertResourceTagsToSet(request.getSystemTags()));

        // Get walWorkspace resource tag from
        logger.log("Fetch resource tags.");
        try {
            desiredTags.addAll(model.getTags());

            String accountId = request.getAwsAccountId();
            String partition = request.getAwsPartition();
            String region = request.getRegion();

            //arn:aws:EMRWALService:us-west-2:123456789123:walNameSpace/walName
            String arn = "arn:" + partition + ":emrwal:" + region + ":" + accountId + ":workspace/" + model.getWALWorkspaceName();

            ListTagsForResourceResponse response = proxyClient.injectCredentialsAndInvokeV2(Translator.translateToReadRequest(arn),
                proxyClient.client()::listTagsForResource);
            previousTags.addAll(Translator.translateFromReadResponse(response, model.getWALWorkspaceName()).getTags());

            final Set<Tag> tagsToRemove = Sets.difference(previousTags, desiredTags);
            final Set<Tag> tagsToAdd = Sets.difference(desiredTags, previousTags);

            UntagResourceRequest untagResourceRequest = Translator.untagResourceRequest(arn, tagsToRemove);
            logger.log("Start removing tags");
            proxyClient.injectCredentialsAndInvokeV2(untagResourceRequest,proxyClient.client()::untagResource);


            TagResourceRequest tagResourceRequest = Translator.tagResourceRequest(arn, tagsToAdd);
            logger.log("Start adding tags");
            proxyClient.injectCredentialsAndInvokeV2(tagResourceRequest,proxyClient.client()::tagResource);

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.SUCCESS)
                .build();
        }
        catch (Exception exception) {
            logger.log(String.format("Failed to update workspace: %s", exception.getMessage()));
            return handleError(OPERATION, exception, proxyClient, model, callbackContext);
        }
    }


}
