# Getting Started

The CloudFormation Resource Provider Package For AWS EMR WALWorkspace

## How To Shape the CFN model

1. Modify the `aws-emr-walworkspace.json`
The RPDK will automatically generate the correct resource model from the schema whenever the project is built via Maven. You can also do this manually with the following command: cfn generate.

## How To RUN the SAM test

### pre-requisite

Install ``sam cli`` on your machine first

### Create the SAM test files

1. `aws-emr-walworkspace/sam-tests/{operation}.json`

   1. Support operation aligns with the supported handler (Create/Delete/List/Update)

### Test the handler

### 
Run command  ``sam local invoke TestEntrypoint --event sam-tests/{handler}.json``. Log is available in your local `rdpg.log` and terminal as well.


## How To Register Resource
Registering a resource in AWS is the process of making it available for public use, allowing others to leverage it for provisioning. 

 * Make the Resource Available: By registering the resource, you enable it to be accessed and utilized by the public within the AWS ecosystem.

 * Update After Code Changes: If you make any modifications to the handler, it's essential to update the registry as well. This ensures that the registry contains your latest logic changes and operates as intended.
   
Here's a step-by-step guide:
1. Package Your Project: Run the Maven package command: ``mvn pkg``
2. Set Up Credentials: Consume the credentials from your designated AWS account. For AWS internal development, simply copy and paste from Isengard. Ensure that the credentials have permissions for all CloudFormation operations.
3. Submit the Resource: Use the following command to submit your resource to the designated region: ``cfn submit --region {designated region}``
4. Keep the Version ID: The output of step 3 will provide a version-id. Make sure to retain this ID, as it will be needed in later steps.
5. [Important] Set the Default Version: To specify CloudFormation to pick the latest version you deployed as the default version, run the following command: ``aws cloudformation set-type-default-version --type RESOURCE --type-name AWS::EMR::WALWorkspace --version-id {version id from step 3} --region {designated region}``
6. [Optional] Revert Default Version for Testing: If this is a test case, remember to revert the default version to its original setting after completion.






