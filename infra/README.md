# IT Support Agent - Infrastructure

AWS CDK (Java) infrastructure for the Intelligent IT Support System.

## Prerequisites

- AWS CLI configured with appropriate credentials
- AWS CDK CLI: `npm install -g aws-cdk`
- Java 21+
- Maven

## Deploy

```bash
cd infra
cdk deploy --all
```

## After Deployment

1. Note the stack outputs (Knowledge Base ID, Data Source ID, Bucket Name)
2. Ingest runbook documents: `cd .. && ./scripts/ingest-runbooks.sh`
3. Set environment variables for the application:
   ```bash
   export BEDROCK_KB_ID=<KnowledgeBaseId output>
   export BEDROCK_KB_DS_ID=<DataSourceId output>
   ```
