# Password Reset and Account Unlock

## Runbook ID
KB-PWD-001

## Symptoms
- User reports "account locked" or "invalid credentials" error
- User cannot log in to workstation, email, or corporate applications
- User forgot password after extended leave

## Diagnosis Steps
1. Look up user account status in Active Directory: `adLookup <username>`
2. Check lockout status and last successful login timestamp
3. Verify user identity through security questions or manager confirmation
4. Check if account is disabled vs. locked (different remediation)

## Resolution Steps
1. If account is locked (too many failed attempts):
   - Unlock the account in Active Directory
   - Advise user to wait 5 minutes before retrying
   - If user forgot password, proceed to step 2
2. If password reset is needed:
   - Generate temporary password via AD admin tools
   - Set "must change at next login" flag
   - Communicate temporary password to user via secure channel
   - Confirm user can log in successfully
3. If account is disabled:
   - Escalate to HR/Security — disabled accounts require manager approval to re-enable

## Escalation Criteria
- Account is disabled (not just locked)
- User cannot verify identity
- Account shows suspicious login attempts from unknown locations
- Password reset fails after 2 attempts

## Related Articles
- KB-VPN-001: VPN Certificate Renewal
- KB-ACC-001: Access Request
