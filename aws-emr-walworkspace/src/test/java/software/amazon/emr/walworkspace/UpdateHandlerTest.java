package software.amazon.emr.walworkspace;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.TestInstance;
import software.amazon.awssdk.services.emrwal.EmrwalClient;
import software.amazon.awssdk.services.emrwal.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.emrwal.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.emrwal.model.TagResourceRequest;
import software.amazon.awssdk.services.emrwal.model.TagResourceResponse;
import software.amazon.awssdk.services.emrwal.model.UntagResourceRequest;
import software.amazon.awssdk.services.emrwal.model.UntagResourceResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestInstance(PER_CLASS)
@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {
    @Mock
    EmrwalClient sdkClient;
    @Mock
    private AmazonWebServicesClientProxy proxy;
    @Mock
    private ProxyClient<EmrwalClient> proxyClient;
    private UpdateHandler handler;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS,
            () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(EmrwalClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
        handler = new UpdateHandler();
    }

    @Test
    public void handleRequest_WithTagging() {
        Set<Tag> tags = new HashSet<>();
        tags.add(new Tag("key", "value"));

        Set<software.amazon.awssdk.services.emrwal.model.Tag> sdkTags = new HashSet<>();
        final ResourceModel model =
            ResourceModel.builder().wALWorkspaceName(WALWORKSPACE_NAME)
                .tags(tags)
                .build();
        final TagResourceResponse tagResourceResponse = TagResourceResponse.builder().build();
        when(sdkClient.tagResource(any(TagResourceRequest.class))).thenReturn(tagResourceResponse);
        final UntagResourceResponse untagResourceResponse = UntagResourceResponse.builder().build();
        when(sdkClient.untagResource(any(UntagResourceRequest.class))).thenReturn(untagResourceResponse);
        final ListTagsForResourceResponse listTagsForResourceResponse = ListTagsForResourceResponse
            .builder().tags(sdkTags).build();
        when(sdkClient.listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(listTagsForResourceResponse);

        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNotNull();
        assertThat(response.getResourceModel().getWALWorkspaceName()).isEqualTo(WALWORKSPACE_NAME);
        assertThat(response.getResourceModel().getTags()).isEqualTo(tags);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test void handleRequest_WithUntagging() {
        Set<Tag> tags = new HashSet<>();

        Set<software.amazon.awssdk.services.emrwal.model.Tag> sdkTags = new HashSet<>();
        sdkTags.add(software.amazon.awssdk.services.emrwal.model.Tag.builder().key("key").value("value").build());
        final ResourceModel model =
            ResourceModel.builder().wALWorkspaceName(WALWORKSPACE_NAME)
                .tags(tags)
                .build();
        final TagResourceResponse tagResourceResponse = TagResourceResponse.builder().build();
        when(sdkClient.tagResource(any(TagResourceRequest.class))).thenReturn(tagResourceResponse);
        final UntagResourceResponse untagResourceResponse = UntagResourceResponse.builder().build();
        when(sdkClient.untagResource(any(UntagResourceRequest.class))).thenReturn(untagResourceResponse);
        final ListTagsForResourceResponse listTagsForResourceResponse = ListTagsForResourceResponse
            .builder().tags(sdkTags).build();
        when(sdkClient.listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(listTagsForResourceResponse);

        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNotNull();
        assertThat(response.getResourceModel().getWALWorkspaceName()).isEqualTo(WALWORKSPACE_NAME);
        assertThat(response.getResourceModel().getTags()).isEqualTo(tags);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
}
