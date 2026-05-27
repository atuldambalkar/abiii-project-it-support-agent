#!/bin/bash
# Uploads runbook documents to S3 and triggers Bedrock Knowledge Base sync.
#
# Usage:
#   ./scripts/ingest-runbooks.sh

set -euo pipefail

BUCKET_NAME="it-support-agent-runbooks-892217934708"
KB_ID="NGCEOIVMYF"
DS_ID="VJI9ICQ42F"

echo "=== IT Support Runbook Ingestion ==="
echo "Bucket:          $BUCKET_NAME"
echo "Knowledge Base:  $KB_ID"
echo "Data Source:     $DS_ID"
echo ""

# Upload runbook documents to S3
echo "Uploading runbook documents to s3://$BUCKET_NAME/runbooks/ ..."
aws s3 sync docs/runbooks/ "s3://$BUCKET_NAME/runbooks/" \
    --exclude ".*" \
    --include "*.md" \
    --include "*.pdf" \
    --delete

echo ""
echo "Upload complete. Starting Knowledge Base sync..."

# Trigger ingestion job
INGESTION_JOB=$(aws bedrock-agent start-ingestion-job \
    --knowledge-base-id "$KB_ID" \
    --data-source-id "$DS_ID" \
    --output json)

JOB_ID=$(echo "$INGESTION_JOB" | python3 -c "import sys,json; print(json.load(sys.stdin)['ingestionJob']['ingestionJobId'])")
echo "Ingestion job started: $JOB_ID"

# Poll for completion
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
