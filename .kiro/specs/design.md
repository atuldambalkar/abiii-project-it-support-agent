# Design Document

## Overview

This document describes the technical architecture and implementation approach for the Intelligent IT Support System, implementing all requirements defined in `requirements.md`. The system is built on Amazon Bedrock AgentCore Runtime with Spring Boot and Spring AI, leveraging Bedrock Guardrails for content safety and AgentCore platform services for memory, identity, and sandboxed execution.

## Architecture

```
+----------------------------------------------------------------------+
|                     Amazon Bedrock AgentCore Runtime                   |
|            (Serverless hosting, auto-scaling, session isolation)       |
|                                                                        |
|  +------------------------------------------------------------------+ |
|  |                Spring Boot Application (Spring AI)                 | |
|  |                                                                    | |
|  |  +----------------+   +-----------------+   +-----------------+  | |
|  |  | REST Controller |   |   ChatClient    |   |  @Tool Methods  |  | |
|  |  |  /api/chat      |-->|  (Claude Sonnet |-->|  adLookup()     |  | |
|  |  |  /api/status    |   |   + Guardrails) |   |  searchKB()     |  | |
|  |  +----------------+   +--------+--------+   |  manageTicket() |  | |
|  |                                 |            |  runDiagnostic()|  | |
|  |                                 v            |  checkPerms()   |  | |
|  |                        +----------------+    +-----------------+  | |
|  |                        | MemoryService  |                          | |
|  |                        | (AgentCore SDK)|                          | |
|  |                        +----------------+                          | |
|  +------------------------------------------------------------------+ |
|                                                                        |
|  +------------------------------------------------------------------+ |
|  |            Bedrock Guardrails (applied at every LLM call)          | |
|  |  PII Masking | Topic Denial | Grounding Check | Content Filter     | |
|  +------------------------------------------------------------------+ |
+----------------------------------------------------------------------+
          |              |               |               |
          v              v               v               v
   +------------+ +------------+ +-------------+ +----------------+
   |  Active    | |  Bedrock   | | ServiceNow  | | AgentCore Code |
   | Directory  | | Knowledge  | |   (ITSM)    | |  Interpreter   |
   |            | |   Bases    | |             | |  (Diagnostics) |
   +------------+ +------------+ +-------------+ +----------------+
```

### Technology Stack

| Layer | Technology | Justification |
|-------|-----------|---------------|
| Language | Java 21 | Enterprise standard; virtual threads for concurrency |
| Framework | Spring Boot 3.x + Spring AI | Production-ready; official AgentCore SDK (GA Apr 2026) |
| LLM | Amazon Bedrock (Claude Sonnet) | Best tool-use reasoning; cost-effective |
| Safety | Bedrock Guardrails | Managed; no custom safety code; console-configurable |
| Runtime | AgentCore Runtime | Serverless; session isolation; auto-scaling |
| Memory | AgentCore Memory SDK | Short + long term; no custom tables |
| Auth | AgentCore Identity | OAuth2 delegation; multi-IDP |
| Sandbox | AgentCore Code Interpreter | Isolated; timeout-controlled |
| Knowledge | Bedrock Knowledge Bases + OpenSearch Serverless | Managed RAG pipeline |
| Frontend | React (Vite) on S3 + CloudFront | Streaming chat UI |
| IaC | AWS CDK (Java) | Type-safe; matches app language |
| CI/CD | CodePipeline + CodeBuild | Managed; CDK-integrated |

## Components and Interfaces

### 1. Spring Boot Application Layer

| Component | Class | Responsibility |
|-----------|-------|---------------|
| REST API | `SupportAgentController` | Accepts `POST /api/chat` with `{sessionId, userId, message}`; returns `Flux<String>` (SSE streaming) |
| Chat Client | `AgentChatClient` | Spring AI `ChatClient` configured with Claude Sonnet model, system prompt, tool bindings, and guardrail ID |
| Tools | `ITSupportTools` | `@Tool`-annotated methods; agent autonomously selects which to call based on reasoning |
| Memory | `AgentCoreMemoryService` | Wraps AgentCore Memory SDK; manages `getConversation()`, `saveResolution()`, `recallPastIssues()` |
| Identity | `IdentityDelegationService` | Wraps AgentCore Identity SDK; obtains user-scoped tokens for downstream system calls |

### 2. AgentCore Platform Layer

| Feature | Configuration | Maps to Requirement |
|---------|--------------|-------------------|
| **Runtime** | Deploys Spring Boot JAR as managed HTTPS endpoint; auto-scales 0 to N | Requirement 7: 100+ concurrent sessions |
| **Memory (Short-term)** | Per-session conversation history; auto-expires after 24h inactivity | Requirement 4: conversation continuity |
| **Memory (Long-term)** | User-scoped issue history; persists indefinitely | Requirement 4: cross-session recall |
| **Identity** | OAuth2 client credentials + user delegation via SAML assertion | Requirement 2: identity delegation |
| **Code Interpreter** | Python sandbox; 30s timeout; command allowlist enforced | Requirement 6: sandboxed diagnostics |
| **Observability** | Distributed traces for all reasoning steps, tool calls, guardrail events | Requirement 7: audit retention |

### 3. Bedrock Guardrails Configuration

| Policy | Type | Configuration | Maps to Requirement |
|--------|------|--------------|-------------------|
| PII Detection | Sensitive info filter | Entities: SSN, EMPLOYEE_ID, ADDRESS, PHONE, EMAIL; Action: MASK | Requirement 3: PII protection |
| Topic Denial | Topic policy | Denied: salary, performance_review, legal_advice, investment; Action: BLOCK + redirect message | Requirement 3: off-topic blocking |
| Content Filter | Word/pattern policy | Regex: `rm -rf`, `DROP TABLE`, `FORMAT`, `del /f`, `shutdown -h`; Action: BLOCK + safe alternative | Requirement 3: destructive command blocking |
| Grounding | Grounding check | Source: Bedrock Knowledge Base; Confidence threshold: 0.7; Action: BLOCK ungrounded content | Requirement 5: grounded troubleshooting |
| Credential Filter | Word policy | Regex for API keys, passwords, connection strings; Action: MASK | Requirement 3: credential leakage |

### 4. Sequence Diagrams

#### Scenario A: Autonomous VPN Resolution (Requirements 1, 5, 6)

```
User          Controller     ChatClient      Guardrails      Tools            Memory
 |                |              |               |              |                |
 |--"Can't VPN"->|              |               |              |                |
 |                |--message---->|               |              |                |
 |                |              |--think------->|              |                |
 |                |              |<--pass--------|              |                |
 |                |              |--call: adLookup("user1")---->|                |
 |                |              |<--{status: active}-----------|                |
 |                |              |--call: searchKB("VPN cert")->|                |
 |                |              |<--[KB article: cert renewal]-|                |
 |                |              |--call: runDiagnostic("cert")->|               |
 |                |              |<--{expired: true, days: 2}---|                |
 |                |              |--call: renewCert("user1")---->|               |
 |                |              |<--{success: true}------------|                |
 |                |              |--generate response--->|       |                |
 |                |              |<--grounding: PASS-----|       |                |
 |                |              |<--PII check: PASS-----|       |                |
 |                |              |--save resolution--------------------------------------------->|
 |<--"Certificate renewed. Please restart your VPN client."-----|                |
```

#### Scenario B: Guardrail Blocks Destructive Request (Requirement 3)

```
User          Controller     ChatClient      Guardrails
 |                |              |               |
 |--"Delete the-->|              |               |
 |  staging DB"   |--message---->|               |
 |                |              |--generate---->|
 |                |              |<--BLOCKED-----|  (content filter: destructive)
 |                |              |--fallback---->|
 |                |              |<--PASS--------|
 |<--"I can't delete databases directly. I've created ticket INC007 for the DBA team."
```

#### Scenario C: Memory Recall Across Sessions (Requirement 4)

```
User          Controller     ChatClient      Memory
 |                |              |              |
 |--"Remember my->|              |              |
 |  Outlook issue" |--message--->|              |
 |                |              |--recall------>|
 |                |              |<--{ticket: INC005, issue: "Outlook crash on attachments",
 |                |              |    resolution: "RAM upgrade", date: "2026-05-19"}
 |                |              |--reason with context-->(LLM)
 |                |              |<--response with continuity--
 |<--"Yes, I see your Outlook issue from last week (INC005). The RAM was upgraded but
 |    you're still having crashes. Let me run some new diagnostics..."
```

## Data Models

### Chat Request/Response

```java
public record ChatRequest(
    String sessionId,
    String userId,
    String message
) {}

// Response is Flux<String> via SSE streaming
```

### AgentCore Memory Schema

```json
{
  "shortTerm": {
    "sessionId": "uuid",
    "messages": [
      {"role": "user|assistant", "content": "string", "timestamp": "ISO8601"}
    ],
    "toolCalls": [
      {"tool": "string", "input": {}, "output": {}, "timestamp": "ISO8601"}
    ]
  },
  "longTerm": {
    "userId": "string",
    "resolvedIssues": [
      {
        "ticketId": "string",
        "summary": "string",
        "resolution": "string",
        "toolsUsed": ["string"],
        "resolvedAt": "ISO8601"
      }
    ]
  }
}
```

## Error Handling

| Error | Strategy |
|-------|----------|
| LLM timeout/throttle | Retry 3x with exponential backoff (1s, 2s, 4s); then return "temporarily unavailable" |
| Tool call failure | Log error; inform user; attempt alternative tool or escalate to human |
| Guardrail BLOCK | Return safe fallback response; log intervention to audit trail |
| Memory retrieval failure | Degrade gracefully; ask user for context instead of hallucinating |
| Identity token expiry | Request re-auth; preserve conversation state during re-auth flow |
| Code Interpreter timeout | Return "diagnostic timed out" with manual steps as alternative |

## Testing Strategy

| Type | Approach | Coverage Target |
|------|----------|----------------|
| Unit | JUnit 5 + Mockito for tool methods and services | 80% line coverage |
| Integration | WireMock (AD, ServiceNow); LocalStack (S3, IAM) | All tool integrations |
| Agent Behavior | AgentCore Evaluations (LLM-as-judge: accuracy, safety, helpfulness) | 5 sample scenarios |
| Guardrail | Dedicated suite: 10 PII inputs, 5 destructive commands, 5 off-topic | 100% block rate |
| Load | Gatling: 50 concurrent sessions, 5 min sustained | p95 < 3s first-token |
| E2E | Playwright for chat frontend + API response assertions | Critical user journeys |

## Correctness Properties

The following properties must hold for the system to be considered correct:

### Property 1: Guardrail Completeness
Every LLM invocation (input and output) passes through Bedrock Guardrails with no bypass path. There is no code path that sends a prompt to the model or returns a response to the user without guardrail evaluation.

**Validates: Requirements 3.5, 7.3**

### Property 2: PII Non-Exposure
No system response delivered to the user contains unmasked PII (SSN, employee ID, home address, phone number, email). If PII is present in source data, it is masked before delivery.

**Validates: Requirements 3.1**

### Property 3: Identity Scope Preservation
The system never performs an action on behalf of a user that exceeds the user's existing permission scope. All downstream tool calls use the requesting user's delegated credentials, not a service-level superuser.

**Validates: Requirements 2.3, 2.5**

### Property 4: Grounding Fidelity
Every troubleshooting step presented to the user is grounded in a Knowledge Base article. If the grounding check fails (confidence < 0.7), the response is blocked and the user is informed that no documented procedure exists.

**Validates: Requirements 5.3, 5.4**

### Property 5: Sandbox Isolation
Diagnostic commands execute only within the AgentCore Code Interpreter sandbox. No diagnostic command can modify production systems, access the host filesystem, or execute commands outside the approved allowlist.

**Validates: Requirements 6.1, 6.4**

### Property 6: Escalation Guarantee
If the system cannot resolve an issue within 3 tool-call attempts, it escalates to a human agent with full context. The system never silently fails or loops indefinitely.

**Validates: Requirements 1.5**

### Property 7: Memory Consistency
Resolved issues saved to long-term memory are retrievable in subsequent sessions. The system never fabricates memory - if no matching memory exists, it asks clarifying questions rather than hallucinating context.

**Validates: Requirements 4.4, 4.5**
