package software.amazon.emr.walworkspace;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import software.amazon.cloudformation.proxy.StdCallbackContext;

@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext {
    public Integer retryAttempts = 5;
    public String walWorkspaceArn;
}
