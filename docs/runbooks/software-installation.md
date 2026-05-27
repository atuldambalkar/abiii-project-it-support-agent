# Software Installation Request

## Runbook ID
KB-SW-001

## Symptoms
- User requests installation of a specific software application
- User needs a newer version of existing software
- Application crashes and requires reinstallation

## Diagnosis Steps
1. Verify the requested software is on the approved software list
2. Check user's device meets minimum system requirements
3. Confirm user has appropriate license allocation
4. Check if software is available in the self-service portal

## Resolution Steps
1. If software is on the approved list and available in self-service:
   - Direct user to Software Center / Self-Service Portal
   - Provide installation instructions specific to the application
2. If software requires admin installation:
   - Create a ServiceNow request ticket (category: Software Install)
   - Assign to Desktop Engineering team
   - Provide estimated completion time (typically 1-2 business days)
3. If software is NOT on the approved list:
   - Inform user the software requires security review
   - Create a ServiceNow request for software approval
   - Escalate to Security team for review
4. If reinstallation needed:
   - Uninstall current version via control panel or package manager
   - Clear application cache/data if applicable
   - Reinstall from approved source

## Escalation Criteria
- Software not on approved list (requires security review)
- License unavailable (requires procurement)
- Installation fails after 2 attempts
- Software requires elevated privileges not available to user

## Related Articles
- KB-ACC-001: Access Request
