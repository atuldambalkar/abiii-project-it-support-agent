package com.example.itsupport.controller;

import com.example.itsupport.agent.AgentChatClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api")
public class SupportAgentController {

    private static final Logger log = LoggerFactory.getLogger(SupportAgentController.class);

    private final AgentChatClient agentChatClient;

    public SupportAgentController(AgentChatClient agentChatClient) {
        this.agentChatClient = agentChatClient;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody ChatRequest request) {
        log.info("Chat request: sessionId={} userId={} message={}",
                request.sessionId(), request.userId(), request.message());
        return agentChatClient.streamResponse(request.sessionId(), request.userId(), request.message())
                .doOnComplete(() -> log.info("Chat response complete: sessionId={}", request.sessionId()))
                .doOnError(e -> log.error("Chat error: sessionId={} error={}", request.sessionId(), e.getMessage()));
    }

    @GetMapping("/status")
    public String status() {
        return "IT Support Agent is running";
    }
}
