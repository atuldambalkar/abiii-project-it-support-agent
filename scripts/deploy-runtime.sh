#!/bin/bash
# Builds the application JAR and deploys to AgentCore Runtime.
#
# Usage:
#   ./scripts/deploy-runtime.sh

set -euo pipefail

REGION="${AWS_REGION:-us-east-1}"
STACK_NAME="ITSupportAgentCoreRuntimeStack"
JAR_NAME="it-support-agent-0.0.1-SNAPSHOT.jar"

echo "=== IT Support Agent Deployment ==="
echo ""

echo "Step 1: Building application JAR..."
mvn clean package -DskipTests -q
echo "  Built: target/$JAR_NAME"

echo "Step 2: Getting deployment configuration..."
ARTIFACT_BUCKET=$(aws cloudformation describe-stacks --stack-name "$STACK_NAME" \
    --query "Stacks[0].Outputs[?OutputKey=='ArtifactBucketName'].OutputValue" --output text 2>/dev/null || echo "")
RUNTIME_ROLE_ARN=$(aws cloudformation describe-stacks --stack-name "$STACK_NAME" \
    --query "Stacks[0].Outputs[?OutputKey=='RuntimeRoleArn'].OutputValue" --output text 2>/dev/null || echo "")

if [ -z "$ARTIFACT_BUCKET" ] || [ "$ARTIFACT_BUCKET" = "None" ]; then
    echo "ERROR: CDK stack not deployed. Run: cd infra && cdk deploy ITSupportAgentCoreRuntimeStack"
    exit 1
fi

echo "  Artifact Bucket: $ARTIFACT_BUCKET"
echo "  Runtime Role:    $RUNTIME_ROLE_ARN"

echo "Step 3: Uploading JAR to S3..."
aws s3 cp "target/$JAR_NAME" "s3://$ARTIFACT_BUCKET/deployments/$JAR_NAME"
echo "  Uploaded to: s3://$ARTIFACT_BUCKET/deployments/$JAR_NAME"

echo ""
echo "=== Deployment artifact ready ==="
echo ""
echo "To test locally:"
echo "  eval \$(aws configure export-credentials --profile datumo+playground-Admin --format env)"
echo "  mvn spring-boot:run"
echo ""
echo "  curl -N -X POST http://localhost:8080/api/chat \\"
echo "    -H 'Content-Type: application/json' \\"
echo "    -d '{\"sessionId\":\"s1\",\"userId\":\"user1\",\"message\":\"My VPN is not working\"}'"
