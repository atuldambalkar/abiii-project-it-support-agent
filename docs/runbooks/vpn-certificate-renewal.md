# VPN Certificate Renewal

## Runbook ID
KB-VPN-001

## Symptoms
- User reports "VPN connection failed" or "certificate expired" error
- GlobalProtect / Cisco AnyConnect shows authentication failure
- User was previously able to connect but now cannot

## Diagnosis Steps
1. Verify user account is active in Active Directory (not locked or disabled)
2. Check VPN certificate expiration date using diagnostic command: `certcheck --user <username>`
3. Confirm the user's device is enrolled in the certificate management system
4. Check if the VPN gateway is reachable: `ping vpn-gateway.corp.example.com`

## Resolution Steps
1. If certificate is expired (within last 30 days):
   - Trigger certificate renewal: `cert-renew --user <username> --type vpn`
   - Wait 60 seconds for propagation
   - Ask user to disconnect and reconnect VPN client
2. If certificate is expired (more than 30 days):
   - Escalate to Security team — requires manual re-enrollment
3. If account is locked:
   - Unlock account in Active Directory
   - Reset VPN certificate after unlock

## Escalation Criteria
- Certificate expired more than 30 days ago
- VPN gateway unreachable (infrastructure issue)
- User's device not enrolled in certificate management
- Multiple users reporting same issue simultaneously (potential outage)

## Related Articles
- KB-PWD-001: Password Reset
- KB-NET-001: Network Connectivity
