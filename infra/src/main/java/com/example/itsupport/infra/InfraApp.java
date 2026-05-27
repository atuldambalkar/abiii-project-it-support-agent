package com.example.itsupport.infra;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class InfraApp {

    public static void main(final String[] args) {
        App app = new App();

        Environment env = Environment.builder()
                .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                .region(System.getenv("CDK_DEFAULT_REGION"))
                .build();

        // Knowledge Base: deployed via scripts/setup-knowledge-base.sh

        // AgentCore Runtime stack
        new AgentCoreRuntimeStack(app, "ITSupportAgentCoreRuntimeStack", StackProps.builder()
                .env(env)
                .description("AgentCore Runtime infrastructure for IT Support Agent")
                .build());

        app.synth();
    }
}
