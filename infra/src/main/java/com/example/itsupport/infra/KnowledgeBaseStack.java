package com.example.itsupport.infra;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.s3.*;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

/**
 * CDK Stack that provisions the Bedrock Knowledge Base infrastructure:
 * - S3 bucket for IT runbook documents
 * - OpenSearch Serverless collection (vector search)
 * - Bedrock Knowledge Base with S3 data source
 * - IAM roles for Bedrock to access S3 and OpenSearch
 */
public class KnowledgeBaseStack extends Stack {

    private final Bucket runbookBucket;

    public KnowledgeBaseStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // S3 bucket for runbook documents
        this.runbookBucket = Bucket.Builder.create(this, "RunbookBucket")
                .bucketName("it-support-agent-runbooks-" + this.getAccount())
                .versioned(true)
                .encryption(BucketEncryption.S3_MANAGED)
                .removalPolicy(RemovalPolicy.RETAIN)
                .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
                .build();

        // IAM role for Bedrock Knowledge Base to access S3
        Role bedrockKbRole = Role.Builder.create(this, "BedrockKBRole")
                .roleName("it-support-bedrock-kb-role")
                .assumedBy(new ServicePrincipal("bedrock.amazonaws.com"))
                .build();

        runbookBucket.grantRead(bedrockKbRole);

        // OpenSearch Serverless collection (vector search type)
        CfnResource ossCollection = CfnResource.Builder.create(this, "RunbookVectorCollection")
                .type("AWS::OpenSearchServerless::Collection")
                .properties(Map.of(
                        "Name", "it-support-runbooks",
                        "Type", "VECTORSEARCH",
                        "Description", "Vector store for IT support runbook embeddings"
                ))
                .build();

        // OpenSearch Serverless access policy
        CfnResource ossAccessPolicy = CfnResource.Builder.create(this, "OSSAccessPolicy")
                .type("AWS::OpenSearchServerless::AccessPolicy")
                .properties(Map.of(
                        "Name", "it-support-kb-access",
                        "Type", "data",
                        "Policy", "[{\"Rules\":[{\"ResourceType\":\"index\",\"Resource\":[\"index/it-support-runbooks/*\"],\"Permission\":[\"aoss:*\"]},{\"ResourceType\":\"collection\",\"Resource\":[\"collection/it-support-runbooks\"],\"Permission\":[\"aoss:*\"]}],\"Principal\":[\"" + bedrockKbRole.getRoleArn() + "\"]}]"
                ))
                .build();

        // OpenSearch Serverless network policy
        CfnResource ossNetworkPolicy = CfnResource.Builder.create(this, "OSSNetworkPolicy")
                .type("AWS::OpenSearchServerless::SecurityPolicy")
                .properties(Map.of(
                        "Name", "it-support-kb-network",
                        "Type", "network",
                        "Policy", "[{\"Rules\":[{\"ResourceType\":\"collection\",\"Resource\":[\"collection/it-support-runbooks\"]},{\"ResourceType\":\"dashboard\",\"Resource\":[\"collection/it-support-runbooks\"]}],\"AllowFromPublic\":true}]"
                ))
                .build();

        // OpenSearch Serverless encryption policy
        CfnResource ossEncryptionPolicy = CfnResource.Builder.create(this, "OSSEncryptionPolicy")
                .type("AWS::OpenSearchServerless::SecurityPolicy")
                .properties(Map.of(
                        "Name", "it-support-kb-encryption",
                        "Type", "encryption",
                        "Policy", "{\"Rules\":[{\"ResourceType\":\"collection\",\"Resource\":[\"collection/it-support-runbooks\"]}],\"AWSOwnedKey\":true}"
                ))
                .build();

        // Bedrock Knowledge Base
        CfnResource knowledgeBase = CfnResource.Builder.create(this, "ITSupportKnowledgeBase")
                .type("AWS::Bedrock::KnowledgeBase")
                .properties(Map.of(
                        "Name", "it-support-runbooks-kb",
                        "Description", "Knowledge base containing IT support runbooks for grounded troubleshooting",
                        "RoleArn", bedrockKbRole.getRoleArn(),
                        "KnowledgeBaseConfiguration", Map.of(
                                "Type", "VECTOR",
                                "VectorKnowledgeBaseConfiguration", Map.of(
                                        "EmbeddingModelArn", "arn:aws:bedrock:" + this.getRegion() + "::foundation-model/amazon.titan-embed-text-v2:0"
                                )
                        ),
                        "StorageConfiguration", Map.of(
                                "Type", "OPENSEARCH_SERVERLESS",
                                "OpensearchServerlessConfiguration", Map.of(
                                        "CollectionArn", ossCollection.getAtt("Arn").toString(),
                                        "VectorIndexName", "runbook-index",
                                        "FieldMapping", Map.of(
                                                "VectorField", "embedding",
                                                "TextField", "text",
                                                "MetadataField", "metadata"
                                        )
                                )
                        )
                ))
                .build();
        knowledgeBase.addDependency(ossCollection);

        // Bedrock Knowledge Base Data Source (S3)
        CfnResource dataSource = CfnResource.Builder.create(this, "RunbookDataSource")
                .type("AWS::Bedrock::DataSource")
                .properties(Map.of(
                        "Name", "runbook-s3-source",
                        "KnowledgeBaseId", knowledgeBase.getAtt("KnowledgeBaseId").toString(),
                        "DataSourceConfiguration", Map.of(
                                "Type", "S3",
                                "S3Configuration", Map.of(
                                        "BucketArn", runbookBucket.getBucketArn(),
                                        "InclusionPrefixes", List.of("runbooks/")
                                )
                        )
                ))
                .build();
        dataSource.addDependency(knowledgeBase);

        // Outputs
        new CfnOutput(this, "RunbookBucketName", CfnOutputProps.builder()
                .value(runbookBucket.getBucketName())
                .description("S3 bucket for IT runbook documents")
                .build());

        new CfnOutput(this, "KnowledgeBaseId", CfnOutputProps.builder()
                .value(knowledgeBase.getAtt("KnowledgeBaseId").toString())
                .description("Bedrock Knowledge Base ID for application configuration")
                .build());

        new CfnOutput(this, "DataSourceId", CfnOutputProps.builder()
                .value(dataSource.getAtt("DataSourceId").toString())
                .description("Knowledge Base Data Source ID for sync operations")
                .build());
    }

    public Bucket getRunbookBucket() {
        return runbookBucket;
    }
}
