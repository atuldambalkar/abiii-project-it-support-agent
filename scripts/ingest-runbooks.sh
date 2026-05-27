#!/bin/bash
# Uploads runbook documents to S3 and triggers Bedrock Knowledge Base sync.
#
# Prerequisites:
#   - AWS CLI configured with appropriate credentials
#   - CDK stack deployed (KnowledgeBaseStack)
#
# Usage:
#   ./scripts/ingest-runbooks.sh [bucket-name] [knowledge-base-id] [data-source-id]

set -euo pipefail

STACK_NAME="ITSupportKnowledgeBaseStack"

if [ $# -ge 3 ]; then
    BUCKET_NAME="$1"
    KB_ID="$2"
    DS_ID="$3"
else
    echo "Fetching stack outputs from CloudFormation..."
    BUCKET_NAME=$(aws cloudformation describe-stacks --stack-name "$STACK_NAME" \
        --query "Stacks[0].Outputs[?OutputKey=='RunbookBucketName'].OutputValue" --output text)
    KB_ID=$(aws cloudformation describe-stacks --stack-name "$STACK_NAME" \
        --query "Stacks[0].Outputs[?OutputKey=='KnowledgeBaseId'].OutputValue" --output text)
    DS_ID=$(aws cloudformation describe-stacks --stack-name "$STACK_NAME" \
        --query "Stacks[0].Outputs[?OutputKey=='DataSourceId'].OutputValue" --output text)
fi

echo "=== IT Support Runbook Ingestion ==="
echo "Bucket:          $BUCKET_NAME"
echo "Knowledge Base:  $KB_ID"
echo "Data Source:     $DS_ID"
echo ""

echo "Uploading runbook documents to s3://$BUCKET_NAME/runbooks/ ..."
aws s3 sync docs/runbooks/ "s3://$BUCKET_NAME/runbooks/" \
    --exclude ".*" \
    --include "*.md" \
    --include "*.pdf" \
    --delete

echo ""
echo "Upload complete. Starting Knowledge Base sync..."

INGESTION_JOB=$(aws bedrock-agent start-ingestion-job \
    --knowledge-base-id "$KB_ID" \
    --data-source-id "$DS_ID" \
    --output json)

JOB_ID=$(echo "$INGESTION_JOB" | python3 -c "import sys,json; print(json.load(sys.stdin)['ingestionJob']['ingestionJobId'])")
echo "Ingestion job started: $JOB_ID"

echo "Waiting for ingestion to complete..."
while true; do
    STATUS=$(aws bedrock-agent get-ingestion-job \
        --knowledge-base-id "$KB_ID" \
        --data-source-id "$DS_ID" \
        --ingestion-job-id "$JOB_ID" \
        --query "ingestionJob.status" --output text)

    echo "  Status: $STATUS"

    if [ "$STATUS" = "COMPLETE" ]; then
        echo ""
        echo "Ingestion complete! Runbooks are now searchable via the Knowledge Base."
        break
    elif [ "$STATUS" = "FAILED" ]; then
        echo "ERROR: Ingestion failed. Check AWS console for details."
        exit 1
    fi

    sleep 10
done
