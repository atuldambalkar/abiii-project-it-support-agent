# Outlook and Email Troubleshooting

## Runbook ID
KB-MAIL-001

## Symptoms
- Outlook crashes on startup or when opening attachments
- Email not syncing (stuck in Outbox, not receiving new mail)
- Calendar invites not appearing or sending
- "Cannot connect to Exchange" error

## Diagnosis Steps
1. Check Exchange Online service health status
2. Verify user's mailbox size (quota may be exceeded)
3. Check Outlook profile integrity
4. Verify network connectivity to Exchange endpoints: `ping outlook.office365.com`
5. Check if issue is specific to Outlook desktop or also affects OWA (Outlook Web App)

## Resolution Steps
1. If Outlook crashes on startup:
   - Start Outlook in Safe Mode: `outlook.exe /safe`
   - If Safe Mode works, disable add-ins one by one to find the culprit
   - If Safe Mode also crashes, repair Office installation
2. If email not syncing:
   - Check mailbox quota: if over 90%, advise user to archive or delete
   - Verify Cached Exchange Mode settings
   - Rebuild OST file: close Outlook, rename `.ost` file, restart Outlook
3. If "Cannot connect to Exchange":
   - Verify network connectivity and proxy settings
   - Test with OWA — if OWA works, issue is client-side
   - Repair Outlook profile or create new profile
4. If calendar issues:
   - Check calendar permissions and sharing settings
   - Verify time zone settings
   - Clear calendar cache

## Escalation Criteria
- Exchange Online service degradation (affects multiple users)
- Mailbox corruption requiring admin intervention
- Issue persists after profile recreation
- Data loss reported (missing emails)

## Related Articles
- KB-NET-001: Network Connectivity
- KB-SW-001: Software Installation (for Office repair)
