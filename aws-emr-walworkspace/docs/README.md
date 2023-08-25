# AWS::EMR::WALWorkspace

Resource schema for AWS::EMR::WALWorkspace Type

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::EMR::WALWorkspace",
    "Properties" : {
        "<a href="#walworkspacename" title="WALWorkspaceName">WALWorkspaceName</a>" : <i>String</i>,
        "<a href="#tags" title="Tags">Tags</a>" : <i>[ <a href="tag.md">Tag</a>, ... ]</i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::EMR::WALWorkspace
Properties:
    <a href="#walworkspacename" title="WALWorkspaceName">WALWorkspaceName</a>: <i>String</i>
    <a href="#tags" title="Tags">Tags</a>: <i>
      - <a href="tag.md">Tag</a></i>
</pre>

## Properties

#### WALWorkspaceName

The name of the emrwal container

_Required_: No

_Type_: String

_Minimum Length_: <code>1</code>

_Maximum Length_: <code>32</code>

_Pattern_: <code>^[a-zA-Z0-9]+$</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### Tags

An array of key-value pairs to apply to this resource.

_Required_: No

_Type_: List of <a href="tag.md">Tag</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the WALWorkspaceName.
