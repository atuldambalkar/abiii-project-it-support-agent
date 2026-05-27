package com.example.itsupport.controller;

/**
 * Request body for the chat endpoint.
 */
public record ChatRequest(
        String sessionId,
        String userId,
        String message
) {}
