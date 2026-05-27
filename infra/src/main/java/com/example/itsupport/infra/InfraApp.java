package com.example.itsupport.infra;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

/**
 * CDK App entry point. Deploys all infrastructure stacks for the IT Support Agent.
 */
public class InfraApp {

    public static void main(final String[] args) {
        App app = new App();

        Environment env = Environment.builder()
                .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                .region(System.getenv("CDK_DEFAULT_REGION"))
                .build();

        new KnowledgeBaseStack(app, "ITSupportKnowledgeBaseStack", StackProps.builder()
                .env(env)
                .description("Bedrock Knowledge Base infrastructure for IT Support Agent")
                .build());

        app.synth();
    }
}
