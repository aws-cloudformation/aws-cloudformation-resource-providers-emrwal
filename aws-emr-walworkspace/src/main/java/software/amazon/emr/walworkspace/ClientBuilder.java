package software.amazon.emr.walworkspace;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.conditions.OrRetryCondition;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;
import software.amazon.awssdk.core.retry.conditions.RetryOnExceptionsCondition;
import software.amazon.awssdk.core.retry.conditions.SdkRetryCondition;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.emrwal.EmrwalClient;
import software.amazon.awssdk.services.emrwal.endpoint.EMRWALEndpointHelper;
import software.amazon.awssdk.services.emrwal.model.WalThrottlingException;
import software.amazon.cloudformation.LambdaWrapper;
import software.amazon.cloudformation.proxy.Logger;

public class ClientBuilder {
  private static EmrwalClient client;
  private static ClientBuilder clientBuilder;
  private static final String walRegion = DefaultAwsRegionProviderChain.builder().build().getRegion().toString();;
  private static final String walEndpoint = EMRWALEndpointHelper.getEndpoint(walRegion);

  Logger logger;


//  private ClientBuilder() {
//    this.walRegion = DefaultAwsRegionProviderChain.builder().build().getRegion().toString();
//    this.walEndpoint = EMRWALEndpointHelper.getEndpoint(walRegion);
//  }

  /**
   * Get the instance of EmrWalClient
   *
   * @return
   */
  public static EmrwalClient getClient() {
    try {
      if (client == null) {
        client = createEMRWALClient();
      }
    } catch (URISyntaxException ui) {
      throw new RuntimeException("Invalid URI captured.", ui);
    }
    return client;
  }
  private static EmrwalClient createEMRWALClient()
    throws URISyntaxException {
    //add the retry for the WalThrottlingException
    Set<Class<? extends Exception>> retryExceptions = new HashSet<>();
    retryExceptions.add(WalThrottlingException.class);

    RetryOnExceptionsCondition retryOnExceptionsCondition =
        RetryOnExceptionsCondition.create(retryExceptions);

    RetryCondition retryCondition = OrRetryCondition.create(
        new RetryCondition[]{SdkRetryCondition.DEFAULT, retryOnExceptionsCondition}
    );

    RetryPolicy retryPolicy = RetryPolicy.builder().retryCondition(retryCondition).build();

    //Create the client
    EmrwalClient emrwalClient = EmrwalClient
        .builder()
        .credentialsProvider(DefaultCredentialsProvider.create())
        .overrideConfiguration(ClientOverrideConfiguration.builder().retryPolicy(retryPolicy).build())
        .endpointOverride(new URI(walEndpoint))
        .region(Region.of(walRegion))
        .httpClient(LambdaWrapper.HTTP_CLIENT)
        .build();

    System.out.println(walEndpoint);

    return emrwalClient;
  }
}
