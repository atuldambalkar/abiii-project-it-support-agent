package com.example.itsupport.infra;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.s3.*;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class AgentCoreRuntimeStack extends Stack {

    public AgentCoreRuntimeStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Bucket artifactBucket = Bucket.Builder.create(this, "ArtifactBucket")
                .versioned(true)
                .encryption(BucketEncryption.S3_MANAGED)
                .removalPolicy(RemovalPolicy.DESTROY)
                .autoDeleteObjects(true)
                .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
                .build();

        Role runtimeRole = Role.Builder.create(this, "AgentCoreRuntimeRole")
                .roleName("it-support-agentcore-runtime-role")
                .assumedBy(new CompositePrincipal(
                        new ServicePrincipal("bedrock.amazonaws.com"),
                        new ServicePrincipal("lambda.amazonaws.com")))
                .managedPolicies(List.of(
                        ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSLambdaBasicExecutionRole")))
                .build();

        runtimeRole.addToPolicy(PolicyStatement.Builder.create()
                .actions(List.of("bedrock:InvokeModel", "bedrock:InvokeModelWithResponseStream", "bedrock:ApplyGuardrail"))
                .resources(List.of(
                        "arn:aws:bedrock:" + this.getRegion() + "::foundation-model/*",
                        "arn:aws:bedrock:" + this.getRegion() + ":" + this.getAccount() + ":guardrail/*"))
                .build());

        runtimeRole.addToPolicy(PolicyStatement.Builder.create()
                .actions(List.of("bedrock:Retrieve", "bedrock:RetrieveAndGenerate"))
                .resources(List.of("arn:aws:bedrock:" + this.getRegion() + ":" + this.getAccount() + ":knowledge-base/*"))
                .build());

        runtimeRole.addToPolicy(PolicyStatement.Builder.create()
                .actions(List.of("aoss:APIAccessAll"))
                .resources(List.of(
                        "arn:aws:aoss:" + this.getRegion() + ":" + this.getAccount() + ":collection/*",
                        "arn:aws:aoss:" + this.getRegion() + ":" + this.getAccount() + ":index/*"))
                .build());

        artifactBucket.grantRead(runtimeRole);

        new CfnOutput(this, "ArtifactBucketName", CfnOutputProps.builder()
                .value(artifactBucket.getBucketName())
                .description("S3 bucket for deployment artifacts")
                .build());

        new CfnOutput(this, "RuntimeRoleArn", CfnOutputProps.builder()
                .value(runtimeRole.getRoleArn())
                .description("AgentCore Runtime execution role ARN")
                .build());
    }
}
