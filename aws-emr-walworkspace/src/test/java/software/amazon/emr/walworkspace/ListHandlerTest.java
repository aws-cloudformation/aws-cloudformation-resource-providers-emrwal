package software.amazon.emr.walworkspace;

import java.time.Duration;
import java.util.ArrayList;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.TestInstance;
import software.amazon.awssdk.services.emrwal.EmrwalClient;
import software.amazon.awssdk.services.emrwal.model.ListWorkspacesRequest;
import software.amazon.awssdk.services.emrwal.model.ListWorkspacesResponse;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@TestInstance(PER_CLASS)
@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<EmrwalClient> proxyClient;

    @Mock
    EmrwalClient sdkClient;

    private ListHandler handler;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(EmrwalClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
        handler = new ListHandler();
    }

    @Test
    public void handleRequest_Success() {
        final ResourceModel model = ResourceModel.builder().build();
        final ListWorkspacesResponse listWorkspacesResponse =  mockedListWorkspacesResponse(false);

        when(sdkClient.listWorkspaces(any(ListWorkspacesRequest.class))).thenReturn(listWorkspacesResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        Assertions.assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getResourceModels().get(0).getWALWorkspaceName()).isEqualTo(WALWORKSPACE_NAME);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_Success_NoResourceFound() {
        final ResourceModel model = ResourceModel.builder().build();
        final ListWorkspacesResponse listWorkspacesResponse =  mockedListWorkspacesResponse(true);

        when(sdkClient.listWorkspaces(any(ListWorkspacesRequest.class))).thenReturn(listWorkspacesResponse);

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(model)
            .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        Assertions.assertThat(response.getResourceModels()).isEmpty();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }


    private ListWorkspacesResponse mockedListWorkspacesResponse(boolean emptyList) {
        if (emptyList) {
            return ListWorkspacesResponse.builder().build();
        }

        return ListWorkspacesResponse.builder()
            .walWorkspaceList(new ArrayList<String>(1) {{
                add(WALWORKSPACE_NAME);
        }}).build();
    }
}
