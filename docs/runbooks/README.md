# IT Support Runbooks

This directory contains the IT support runbook corpus that is ingested into the Bedrock Knowledge Base. The agent uses these documents for grounded troubleshooting (Requirement 5).

## Document Structure

Each runbook follows a consistent format:
- **Runbook ID** — Unique identifier (e.g., KB-VPN-001)
- **Symptoms** — What the user reports
- **Diagnosis Steps** — How to investigate the issue
- **Resolution Steps** — How to fix it
- **Escalation Criteria** — When to hand off to a human
- **Related Articles** — Cross-references to other runbooks

## Available Runbooks

| ID | Title | Covers |
|----|-------|--------|
| KB-VPN-001 | VPN Certificate Renewal | Expired certs, connection failures |
| KB-PWD-001 | Password Reset | Account lockout, forgotten passwords |
| KB-SW-001 | Software Installation | Approved software, reinstalls |
| KB-NET-001 | Network Connectivity | DNS, proxy, Wi-Fi issues |
| KB-MAIL-001 | Outlook Troubleshooting | Crashes, sync issues, Exchange errors |
| KB-ACC-001 | Access Request | Permissions, provisioning, denials |

## Ingestion

To upload these documents to the Bedrock Knowledge Base:

```bash
./scripts/ingest-runbooks.sh
```

## Adding New Runbooks

1. Create a new `.md` file in this directory following the template above
2. Run the ingestion script to sync with the Knowledge Base
3. The agent will automatically use the new content for grounded responses
