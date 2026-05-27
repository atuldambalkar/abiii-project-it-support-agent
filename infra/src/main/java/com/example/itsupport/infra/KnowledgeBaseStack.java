package com.example.itsupport.infra;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.s3.*;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class KnowledgeBaseStack extends Stack {

    private final Bucket runbookBucket;

    public KnowledgeBaseStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        this.runbookBucket = Bucket.Builder.create(this, "RunbookBucket")
                .versioned(true)
                .encryption(BucketEncryption.S3_MANAGED)
                .removalPolicy(RemovalPolicy.RETAIN)
                .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
                .build();

        Role bedrockKbRole = Role.Builder.create(this, "BedrockKBRole")
                .roleName("it-support-bedrock-kb-role")
                .assumedBy(new ServicePrincipal("bedrock.amazonaws.com"))
                .build();

        runbookBucket.grantRead(bedrockKbRole);
        bedrockKbRole.addToPolicy(PolicyStatement.Builder.create()
                .actions(List.of("aoss:APIAccessAll"))
                .resources(List.of("arn:aws:aoss:" + this.getRegion() + ":" + this.getAccount() + ":collection/*"))
                .build());

        CfnResource ossEncryptionPolicy = CfnResource.Builder.create(this, "OSSEncryptionPolicy")
                .type("AWS::OpenSearchServerless::SecurityPolicy")
                .properties(Map.of("Name", "it-support-kb-encryption", "Type", "encryption",
                        "Policy", "{\"Rules\":[{\"ResourceType\":\"collection\",\"Resource\":[\"collection/it-support-runbooks\"]}],\"AWSOwnedKey\":true}"))
                .build();

        CfnResource ossNetworkPolicy = CfnResource.Builder.create(this, "OSSNetworkPolicy")
                .type("AWS::OpenSearchServerless::SecurityPolicy")
                .properties(Map.of("Name", "it-support-kb-network", "Type", "network",
                        "Policy", "[{\"Rules\":[{\"ResourceType\":\"collection\",\"Resource\":[\"collection/it-support-runbooks\"]},{\"ResourceType\":\"dashboard\",\"Resource\":[\"collection/it-support-runbooks\"]}],\"AllowFromPublic\":true}]"))
                .build();

        CfnResource ossCollection = CfnResource.Builder.create(this, "RunbookVectorCollection")
                .type("AWS::OpenSearchServerless::Collection")
                .properties(Map.of("Name", "it-support-runbooks", "Type", "VECTORSEARCH",
                        "Description", "Vector store for IT support runbook embeddings"))
                .build();
        ossCollection.addDependency(ossEncryptionPolicy);
        ossCollection.addDependency(ossNetworkPolicy);

        Role indexCreatorRole = Role.Builder.create(this, "IndexCreatorRole")
                .assumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .managedPolicies(List.of(ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSLambdaBasicExecutionRole")))
                .build();
        indexCreatorRole.addToPolicy(PolicyStatement.Builder.create()
                .actions(List.of("aoss:APIAccessAll"))
                .resources(List.of("arn:aws:aoss:" + this.getRegion() + ":" + this.getAccount() + ":collection/*"))
                .build());

        // Data access policy with aoss:* permissions for both Lambda and Bedrock roles
        CfnResource ossAccessPolicy = CfnResource.Builder.create(this, "OSSAccessPolicy")
                .type("AWS::OpenSearchServerless::AccessPolicy")
                .properties(Map.of("Name", "it-support-kb-access", "Type", "data",
                        "Policy", "[{\"Rules\":[{\"ResourceType\":\"index\",\"Resource\":[\"index/it-support-runbooks/*\"],\"Permission\":[\"aoss:*\"]},{\"ResourceType\":\"collection\",\"Resource\":[\"collection/it-support-runbooks\"],\"Permission\":[\"aoss:*\"]}],\"Principal\":[\"" + bedrockKbRole.getRoleArn() + "\",\"" + indexCreatorRole.getRoleArn() + "\"]}]"))
                .build();

        Function indexCreatorFn = Function.Builder.create(this, "IndexCreatorFunction")
                .runtime(Runtime.PYTHON_3_12)
                .handler("index.handler")
                .timeout(Duration.minutes(5))
                .memorySize(256)
                .role(indexCreatorRole)
                .code(Code.fromInline(getIndexCreatorCode()))
                .build();

        CfnResource createIndex = CfnResource.Builder.create(this, "CreateVectorIndex")
                .type("AWS::CloudFormation::CustomResource")
                .properties(Map.of(
                        "ServiceToken", indexCreatorFn.getFunctionArn(),
                        "CollectionEndpoint", ossCollection.getAtt("CollectionEndpoint").toString(),
                        "IndexName", "runbook-index",
                        "EmbeddingDimension", "1024"))
                .build();
        createIndex.addDependency(ossCollection);
        createIndex.addDependency(ossAccessPolicy);

        CfnResource knowledgeBase = CfnResource.Builder.create(this, "ITSupportKnowledgeBase")
                .type("AWS::Bedrock::KnowledgeBase")
                .properties(Map.of(
                        "Name", "it-support-runbooks-kb",
                        "Description", "Knowledge base for IT support runbooks",
                        "RoleArn", bedrockKbRole.getRoleArn(),
                        "KnowledgeBaseConfiguration", Map.of("Type", "VECTOR",
                                "VectorKnowledgeBaseConfiguration", Map.of(
                                        "EmbeddingModelArn", "arn:aws:bedrock:" + this.getRegion() + "::foundation-model/amazon.titan-embed-text-v2:0")),
                        "StorageConfiguration", Map.of("Type", "OPENSEARCH_SERVERLESS",
                                "OpensearchServerlessConfiguration", Map.of(
                                        "CollectionArn", ossCollection.getAtt("Arn").toString(),
                                        "VectorIndexName", "runbook-index",
                                        "FieldMapping", Map.of("VectorField", "embedding", "TextField", "text", "MetadataField", "metadata")))))
                .build();
        knowledgeBase.addDependency(createIndex);

        CfnResource dataSource = CfnResource.Builder.create(this, "RunbookDataSource")
                .type("AWS::Bedrock::DataSource")
                .properties(Map.of("Name", "runbook-s3-source",
                        "KnowledgeBaseId", knowledgeBase.getAtt("KnowledgeBaseId").toString(),
                        "DataSourceConfiguration", Map.of("Type", "S3",
                                "S3Configuration", Map.of("BucketArn", runbookBucket.getBucketArn(),
                                        "InclusionPrefixes", List.of("runbooks/")))))
                .build();
        dataSource.addDependency(knowledgeBase);

        new CfnOutput(this, "RunbookBucketName", CfnOutputProps.builder().value(runbookBucket.getBucketName()).build());
        new CfnOutput(this, "KnowledgeBaseId", CfnOutputProps.builder().value(knowledgeBase.getAtt("KnowledgeBaseId").toString()).build());
        new CfnOutput(this, "DataSourceId", CfnOutputProps.builder().value(dataSource.getAtt("DataSourceId").toString()).build());
    }

    public Bucket getRunbookBucket() { return runbookBucket; }

    private String getIndexCreatorCode() {
        return "import json, urllib.request, urllib.error, time, boto3\n" +
               "from botocore.auth import SigV4Auth\n" +
               "from botocore.awsrequest import AWSRequest\n" +
               "import cfnresponse\n\n" +
               "def handler(event, context):\n" +
               "    try:\n" +
               "        if event['RequestType'] == 'Delete':\n" +
               "            cfnresponse.send(event, context, cfnresponse.SUCCESS, {})\n" +
               "            return\n" +
               "        endpoint = event['ResourceProperties']['CollectionEndpoint']\n" +
               "        index_name = event['ResourceProperties']['IndexName']\n" +
               "        dimension = int(event['ResourceProperties']['EmbeddingDimension'])\n" +
               "        url = f'{endpoint}/{index_name}'\n" +
               "        index_body = json.dumps({'settings':{'index':{'knn':True,'knn.algo_param.ef_search':512}},'mappings':{'properties':{'embedding':{'type':'knn_vector','dimension':dimension,'method':{'engine':'faiss','name':'hnsw','parameters':{'m':16,'ef_construction':512},'space_type':'l2'}},'text':{'type':'text'},'metadata':{'type':'text'}}}})\n" +
               "        session = boto3.Session()\n" +
               "        creds = session.get_credentials().get_frozen_credentials()\n" +
               "        region = session.region_name\n" +
               "        # Retry with increasing backoff - access policy propagation can take 2-3 min\n" +
               "        max_attempts = 8\n" +
               "        for attempt in range(max_attempts):\n" +
               "            wait_time = 30 * (attempt + 1)\n" +
               "            print(f'Attempt {attempt+1}/{max_attempts}: waiting {wait_time}s...')\n" +
               "            time.sleep(wait_time)\n" +
               "            try:\n" +
               "                put_req = AWSRequest(method='PUT', url=url, data=index_body, headers={'Content-Type':'application/json'})\n" +
               "                SigV4Auth(creds, 'aoss', region).add_auth(put_req)\n" +
               "                r = urllib.request.Request(url, data=index_body.encode(), method='PUT', headers=dict(put_req.headers))\n" +
               "                resp = urllib.request.urlopen(r)\n" +
               "                result = json.loads(resp.read().decode())\n" +
               "                print(f'Index created successfully: {result}')\n" +
               "                cfnresponse.send(event, context, cfnresponse.SUCCESS, {'IndexName':index_name})\n" +
               "                return\n" +
               "            except urllib.error.HTTPError as e:\n" +
               "                body = e.read().decode() if e.fp else ''\n" +
               "                print(f'HTTP {e.code}: {body}')\n" +
               "                if e.code == 403 and attempt < max_attempts - 1:\n" +
               "                    print('Access policy not yet propagated, retrying...')\n" +
               "                    continue\n" +
               "                elif e.code == 400 and 'resource_already_exists' in body:\n" +
               "                    print(f'Index {index_name} already exists')\n" +
               "                    cfnresponse.send(event, context, cfnresponse.SUCCESS, {'IndexName':index_name})\n" +
               "                    return\n" +
               "                raise\n" +
               "        cfnresponse.send(event, context, cfnresponse.FAILED, {'Error':'Max retries exceeded - access policy did not propagate'})\n" +
               "    except Exception as e:\n" +
               "        print(f'Error: {str(e)}')\n" +
               "        cfnresponse.send(event, context, cfnresponse.FAILED, {'Error':str(e)})\n";
    }
}
