#!/bin/bash
# Deletes all resources created by setup-knowledge-base.sh
#
# Usage:
#   ./scripts/teardown-knowledge-base.sh

set -euo pipefail

REGION="${AWS_REGION:-us-east-1}"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
COLLECTION_NAME="it-support-runbooks"
KB_NAME="it-support-runbooks-kb"
BUCKET_NAME="it-support-agent-runbooks-${ACCOUNT_ID}"
ROLE_NAME="it-support-bedrock-kb-role"

echo "=== IT Support Knowledge Base Teardown ==="
echo "Region:     $REGION"
echo "Account:    $ACCOUNT_ID"
echo ""

echo "Step 1: Deleting Bedrock Data Source..."
KB_ID=$(aws bedrock-agent list-knowledge-bases \
    --query "knowledgeBaseSummaries[?name=='${KB_NAME}'].knowledgeBaseId" --output text 2>/dev/null || echo "")

if [ -n "$KB_ID" ] && [ "$KB_ID" != "None" ]; then
    DS_ID=$(aws bedrock-agent list-data-sources \
        --knowledge-base-id "$KB_ID" \
        --query "dataSourceSummaries[0].dataSourceId" --output text 2>/dev/null || echo "")
    if [ -n "$DS_ID" ] && [ "$DS_ID" != "None" ]; then
        aws bedrock-agent delete-data-source --knowledge-base-id "$KB_ID" --data-source-id "$DS_ID" 2>/dev/null || true
        echo "  Deleted data source: $DS_ID"
    fi
fi

echo "Step 2: Deleting Bedrock Knowledge Base..."
if [ -n "$KB_ID" ] && [ "$KB_ID" != "None" ]; then
    aws bedrock-agent delete-knowledge-base --knowledge-base-id "$KB_ID" 2>/dev/null || true
    echo "  Deleted: $KB_ID"
fi

echo "Step 3: Deleting OpenSearch Serverless collection..."
COLLECTION_ID=$(aws opensearchserverless list-collections \
    --query "collectionSummaries[?name=='${COLLECTION_NAME}'].id" --output text 2>/dev/null || echo "")
if [ -n "$COLLECTION_ID" ] && [ "$COLLECTION_ID" != "None" ]; then
    aws opensearchserverless delete-collection --id "$COLLECTION_ID" 2>/dev/null || true
    echo "  Deleted: $COLLECTION_ID"
    sleep 30
fi

echo "Step 4: Deleting access policy..."
aws opensearchserverless delete-access-policy --name "${COLLECTION_NAME}-access" --type data 2>/dev/null || true

echo "Step 5: Deleting network policy..."
aws opensearchserverless delete-security-policy --name "${COLLECTION_NAME}-network" --type network 2>/dev/null || true

echo "Step 6: Deleting encryption policy..."
aws opensearchserverless delete-security-policy --name "${COLLECTION_NAME}-encryption" --type encryption 2>/dev/null || true

echo "Step 7: Deleting IAM role..."
aws iam delete-role-policy --role-name "$ROLE_NAME" --policy-name "bedrock-kb-access" 2>/dev/null || true
aws iam delete-role --role-name "$ROLE_NAME" 2>/dev/null || true

echo "Step 8: Deleting S3 bucket..."
if aws s3api head-bucket --bucket "$BUCKET_NAME" 2>/dev/null; then
    aws s3 rm "s3://${BUCKET_NAME}" --recursive
    aws s3api delete-bucket --bucket "$BUCKET_NAME"
    echo "  Deleted: $BUCKET_NAME"
fi

echo ""
echo "=== Teardown Complete ==="
