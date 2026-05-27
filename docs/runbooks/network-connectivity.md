# Network Connectivity Troubleshooting

## Runbook ID
KB-NET-001

## Symptoms
- User reports "no internet" or "cannot access internal sites"
- Slow network performance or intermittent connectivity
- Specific applications fail to connect while others work
- DNS resolution failures

## Diagnosis Steps
1. Check if issue is local (single user) or widespread (multiple users/sites)
2. Run basic connectivity diagnostics:
   - `ping 8.8.8.8` (test raw connectivity)
   - `nslookup corp.example.com` (test DNS resolution)
   - `traceroute internal-app.corp.example.com` (identify routing issues)
3. Check proxy configuration: `echo $http_proxy` or system proxy settings
4. Verify DHCP lease and IP assignment: `ipconfig /all` or `ifconfig`
5. Check if VPN is connected (if remote) — some resources require VPN

## Resolution Steps
1. If DNS resolution fails:
   - Flush DNS cache: `ipconfig /flushdns` (Windows) or `sudo dscacheutil -flushcache` (macOS)
   - Verify DNS server settings point to corporate DNS
   - If using VPN, ensure split-tunnel DNS is configured correctly
2. If no connectivity at all:
   - Release and renew DHCP lease
   - Disable and re-enable network adapter
   - Check physical cable connection (if wired)
   - Forget and reconnect to Wi-Fi network
3. If specific apps fail:
   - Check if app requires VPN and VPN is connected
   - Verify proxy exceptions for the application
   - Check firewall rules: `netstat -an | grep <port>`
4. If slow performance:
   - Run speed test to baseline bandwidth
   - Check for bandwidth-heavy processes: `netstat -b` or Activity Monitor
   - If on Wi-Fi, check signal strength and try moving closer to AP

## Escalation Criteria
- Multiple users affected (potential infrastructure issue)
- Network outage lasting more than 15 minutes
- Suspected security incident (unusual traffic patterns)
- Hardware failure (switch, router, access point)

## Related Articles
- KB-VPN-001: VPN Certificate Renewal
- KB-MAIL-001: Outlook Troubleshooting
