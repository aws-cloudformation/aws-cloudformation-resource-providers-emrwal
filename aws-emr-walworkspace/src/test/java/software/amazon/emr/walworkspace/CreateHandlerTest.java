package software.amazon.emr.walworkspace;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.TestInstance;
import software.amazon.awssdk.services.emrwal.EmrwalClient;
import software.amazon.awssdk.services.emrwal.model.CreateWorkspaceRequest;
import software.amazon.awssdk.services.emrwal.model.CreateWorkspaceResponse;
import software.amazon.awssdk.services.emrwal.model.EmrwalException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
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
public class CreateHandlerTest extends AbstractTestBase {

    @Mock
    EmrwalClient sdkClient;
    @Mock
    private AmazonWebServicesClientProxy proxy;
    @Mock
    private ProxyClient<EmrwalClient> proxyClient;
    private CreateHandler handler;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS,
            () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(EmrwalClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
        handler = new CreateHandler();
    }

    @Test
    public void handRequest_success() {
        final ResourceModel model =
            ResourceModel.builder().wALWorkspaceName(WALWORKSPACE_NAME)
                .build();
        final CreateWorkspaceResponse createWorkspaceResponse = CreateWorkspaceResponse.builder().build();
        when(sdkClient.createWorkspace(any(CreateWorkspaceRequest.class))).thenReturn(
            createWorkspaceResponse);

        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();
        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        Assertions.assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel()).isEqualToComparingFieldByField(model);
    }

    @Test
    public void handRequest_tagOnCreate_success() {
        Set<Tag> tags = new HashSet<>();
        tags.add(new Tag("key", "value"));
        final ResourceModel model =
            ResourceModel.builder().wALWorkspaceName(WALWORKSPACE_NAME)
                .tags(tags)
                .build();
        final CreateWorkspaceResponse createWorkspaceResponse = CreateWorkspaceResponse.builder().build();
        when(sdkClient.createWorkspace(any(CreateWorkspaceRequest.class))).thenReturn(
            createWorkspaceResponse);

        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();
        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        Assertions.assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel()).isEqualToComparingFieldByField(model);
    }

    @Test
    public void handleRequest_FailureWithInternalServiceCreationError() {
        Set<Tag> tags = new HashSet<>();
        tags.add(new Tag("key", "value"));
        final ResourceModel model =
            ResourceModel.builder().wALWorkspaceName(WALWORKSPACE_NAME)
                .tags(tags)
                .build();
        final CreateWorkspaceResponse createWorkspaceResponse = CreateWorkspaceResponse.builder().build();

        when(sdkClient.createWorkspace(any(CreateWorkspaceRequest.class)))
            .thenThrow(EmrwalException.builder()
                .requestId("1234567")
                .message("something went wrong")
                .build());

        final ResourceHandlerRequest<ResourceModel> request =
            ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);
        Assertions.assertThat(response.getResourceModels()).isNull();
    }
}