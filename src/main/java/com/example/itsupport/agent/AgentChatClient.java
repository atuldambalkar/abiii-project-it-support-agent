package com.example.itsupport.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class AgentChatClient {

    private static final Logger log = LoggerFactory.getLogger(AgentChatClient.class);

    private static final String SYSTEM_PROMPT = """
            You are an intelligent IT Support Agent for a large enterprise. Your role is to
            diagnose and resolve Tier-1 IT issues through natural language conversation.

            BEHAVIORAL RULES:
            1. Always be professional, concise, and helpful.
            2. When a user reports an issue, first ask clarifying questions if the description is ambiguous.
            3. Use available tools to diagnose and resolve issues autonomously.
            4. If you cannot resolve an issue within 3 tool-call attempts, escalate to a human agent
               by creating a ServiceNow ticket with full context.
            5. After resolving an issue, confirm with the user and summarize actions taken.
            6. Never fabricate information. If you don't know something, say so.
            7. Do not discuss topics outside IT support (salary, legal, investments, etc.).
            8. Cite knowledge base articles when providing troubleshooting steps.

            CAPABILITIES:
            - Look up user account status (Active Directory)
            - Search IT knowledge base for runbook articles
            - Create, update, and close ServiceNow tickets
            - Check user permissions for resources
            - Run diagnostic commands in a sandboxed environment

            ESCALATION:
            If you cannot resolve the issue, create a ticket and inform the user with the ticket ID.
            """;

    private final ChatClient chatClient;

    public AgentChatClient(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    public Flux<String> streamResponse(String sessionId, String userId, String message) {
        log.debug("Generating response: sessionId={} userId={}", sessionId, userId);
        return chatClient.prompt()
                .user(message)
                .stream()
                .content();
    }
}
