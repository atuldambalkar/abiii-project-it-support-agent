# Access Request and Provisioning

## Runbook ID
KB-ACC-001

## Symptoms
- User requests access to a shared drive, application, or cloud resource
- User receives "Access Denied" or "403 Forbidden" when accessing a resource
- New employee needs initial access provisioning
- User changing roles needs updated permissions

## Diagnosis Steps
1. Identify the specific resource the user needs access to
2. Check current user permissions: `checkPermissions --user <username> --resource <resource>`
3. Verify the resource's access policy (who can grant access)
4. Confirm user's role/department matches the access requirements
5. Check if access requires manager approval

## Resolution Steps
1. If user has a valid business need and resource allows self-service:
   - Direct user to the access request portal
   - Provide the resource name and access level needed
2. If access requires approval:
   - Create ServiceNow access request ticket
   - Route to resource owner for approval
   - Notify user of expected approval timeline (typically 1-3 business days)
3. If user is denied access:
   - Explain which policy prevents access
   - Suggest alternative resources or escalation path
   - Document the denial reason in the ticket
4. For new employee provisioning:
   - Verify onboarding ticket exists with role-based access list
   - Provision standard access per role template
   - Confirm all systems accessible after provisioning

## Escalation Criteria
- Emergency access needed for production incident response
- Access policy unclear or conflicting
- Resource owner unresponsive after 3 business days
- Suspected unauthorized access attempt

## Related Articles
- KB-PWD-001: Password Reset
- KB-SW-001: Software Installation
