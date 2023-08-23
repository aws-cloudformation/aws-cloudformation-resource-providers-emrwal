package software.amazon.emr.walworkspace;

import com.google.common.collect.Sets;
import software.amazon.awssdk.services.emrwal.model.CreateWorkspaceRequest;
import software.amazon.awssdk.services.emrwal.model.DeleteWorkspaceRequest;
import software.amazon.awssdk.services.emrwal.model.EmrwalException;
import software.amazon.awssdk.services.emrwal.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.emrwal.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.emrwal.model.ListWorkspacesRequest;
import software.amazon.awssdk.services.emrwal.model.ListWorkspacesResponse;
import software.amazon.awssdk.services.emrwal.model.ResourceNotFoundException;
import software.amazon.awssdk.services.emrwal.model.TagResourceRequest;
import software.amazon.awssdk.services.emrwal.model.TooManyTagsException;
import software.amazon.awssdk.services.emrwal.model.UntagResourceRequest;
import software.amazon.awssdk.services.emrwal.model.WalThrottlingException;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {

  /**
   * Request to create a workspace
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  public static CreateWorkspaceRequest translateToCreateRequest(final ResourceModel model) {
    CreateWorkspaceRequest createWorkspaceRequest;

    if(model.getTags() != null) {
      Set<software.amazon.awssdk.services.emrwal.model.Tag> tags =
          model.getTags().stream().map(resourceModelTag -> TagHelper.toSDKTag(resourceModelTag))
                  .collect(Collectors.toSet());
      createWorkspaceRequest = CreateWorkspaceRequest.builder().tags(tags).walWorkspace(
          model.getWALWorkspaceName()).build();
    } else {
      createWorkspaceRequest = CreateWorkspaceRequest.builder().walWorkspace(model.getWALWorkspaceName()).build();
    }
    return createWorkspaceRequest;
  }

  /**
   * Request to read a resource
   * Since we do not have a dedicated API for describe a walworkspace, we will
   * query the associated default tag for a given resource.
   * If there is no default tag return that means the resource is not existing.
   *
   * @param arn resource model
   * @return ListTagsForResourceRequest the aws service request to describe a resource
   */
  public static ListTagsForResourceRequest translateToReadRequest(final String arn) {
    return ListTagsForResourceRequest.builder().resourceARN(arn).build();
  }

  /**
   * Translates resource object from sdk into a resource model
   * @return model resource model
   */
  public static ResourceModel translateFromReadResponse(final ListTagsForResourceResponse response,
      final String walWorkspaceName) {
    Set<Tag> tags = response.tags().stream().map(sdkTag -> TagHelper.toResourceModelTag(sdkTag)).collect(
        Collectors.toSet());

    return ResourceModel.builder()
        .wALWorkspaceName(walWorkspaceName)
        .tags(tags)
        .build();
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return DeleteWorkspaceRequest the aws service request to delete a resource
   */
  public static DeleteWorkspaceRequest translateToDeleteRequest(final ResourceModel model) {
    return DeleteWorkspaceRequest.builder().walWorkspace(model.getWALWorkspaceName()).build();
  }


  /**
   * Request to list resources
   * @param nextToken token passed to the aws service list resources request
   * @return awsRequest the aws service request to list resources within aws account
   */
  public static ListWorkspacesRequest translateToListRequest(final int maxResult, final String nextToken) {
    return ListWorkspacesRequest.builder()
        .nextToken(nextToken)
        .maxResults(maxResult)
        .build();
  }

  /**
   * Request to add tags to a resource
   * @param arn workspace arn
   * @param tagsToAdd
   *
   * @return TagResourceRequest the aws service request for tagging
   */
  public static TagResourceRequest tagResourceRequest(final String arn, final Set<Tag> tagsToAdd) {
    return TagResourceRequest.builder()
        .resourceARN(arn)
        .tags(tagsToAdd.stream()
            .map(resourceModelTag -> TagHelper.toSDKTag(resourceModelTag))
            .collect(Collectors.toList()))
        .build();
  }

  /**
   * Request to remove tags from a resource
   * @param arn workspace arn
   * @param tagsToRemove
   *
   * @return UntagResourceRequest the aws service request for untagging
   */
  public static UntagResourceRequest untagResourceRequest(final String arn, final Set<Tag> tagsToRemove) {
    return UntagResourceRequest.builder()
        .resourceARN(arn)
        .tagKeys(
            tagsToRemove.stream()
                .map(resourceModelTag -> resourceModelTag.getKey())
                .collect(Collectors.toList())).build();
  }

  /**
   * Translates workspace list to a list of resource models.
   *
   *  @param listWorkspacesResponse
   * @return list of resource models
   */
  public static List<ResourceModel> translateFromListResponse(final ListWorkspacesResponse listWorkspacesResponse) {
    return streamOfOrEmpty(listWorkspacesResponse.walWorkspaceList())
        .map(walWorkspaceName -> ResourceModel.builder()
            .wALWorkspaceName(walWorkspaceName)
            .build())
        .collect(Collectors.toList());
  }

  private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection)
        .map(Collection::stream)
        .orElseGet(Stream::empty);
  }

  /**
   * Translate emrwal exceptions to cloud-formation handler exceptions.
   *
   * @param exception  EMRWAL exception
   * @param operation  Operation for which the exception is thrown.
   * @return Translated cfn handler exception
   */

  static BaseHandlerException translate(final EmrwalException exception, final String operation, final String workSpaceName,
      final CallbackContext callbackContext) {
    return Optional.ofNullable(exception)
        .map(e -> {
          // list operation does not require arn in the input. Handle this scenario separately
          if("AWS-EMR-WALWorkspace::List".equals(operation)) {
            return new CfnGeneralServiceException(operation, e);
          } else if (e instanceof ResourceNotFoundException) {
            return StringUtils.isEmpty(workSpaceName)
                ? new CfnNotFoundException(e)
                : new CfnNotFoundException(ResourceModel.TYPE_NAME, workSpaceName);
          } else if (e instanceof TooManyTagsException) {
            return new CfnServiceInternalErrorException(operation, e);
          } else if (e instanceof WalThrottlingException) {
            return new CfnThrottlingException(operation, e);
          } else {
            return new CfnGeneralServiceException(operation, e);
          }
        })
        .orElse(null);
  }

  static Set<Tag> convertResourceTagsToSet(Map<String, String> resourceTags) {
    Set<Tag> tags = new HashSet<>();
    if (resourceTags != null) {
      resourceTags.forEach((key, value) -> tags.add(Tag.builder().key(key).value(value).build()));
    }
    return tags;
  }
}
