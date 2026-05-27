package com.example.itsupport.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service responsible for obtaining user-scoped or service-level OAuth2 tokens
 * for downstream system calls (Active Directory, ServiceNow).
 */
@Service
public class IdentityDelegationService {

    private static final Logger log = LoggerFactory.getLogger(IdentityDelegationService.class);

    public static final String TARGET_ACTIVE_DIRECTORY = "active-directory";
    public static final String TARGET_SERVICENOW = "servicenow";

    private final ReactiveOAuth2AuthorizedClientManager authorizedClientManager;

    public IdentityDelegationService(ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizedClientManager = authorizedClientManager;
    }

    public Mono<String> getTokenForUser(String userId, String targetSystem) {
        log.info("Acquiring delegated token for user={} target={}", userId, targetSystem);
        validateTargetSystem(targetSystem);
        return acquireToken(targetSystem, userId)
                .doOnNext(token -> log.debug("Token acquired for user={} target={} expires={}",
                        userId, targetSystem, token.getExpiresAt()))
                .map(OAuth2AccessToken::getTokenValue);
    }

    public Mono<String> getServiceToken(String targetSystem) {
        log.info("Acquiring service token for target={}", targetSystem);
        validateTargetSystem(targetSystem);
        return acquireToken(targetSystem, "service-principal")
                .map(OAuth2AccessToken::getTokenValue);
    }

    private Mono<OAuth2AccessToken> acquireToken(String registrationId, String principal) {
        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
                .withClientRegistrationId(registrationId)
                .principal(principal)
                .build();
        return authorizedClientManager.authorize(request)
                .switchIfEmpty(Mono.error(new IdentityDelegationException(
                        "Failed to acquire token for registration=" + registrationId
                                + " principal=" + principal)))
                .map(OAuth2AuthorizedClient::getAccessToken);
    }

    private void validateTargetSystem(String targetSystem) {
        if (!TARGET_ACTIVE_DIRECTORY.equals(targetSystem)
                && !TARGET_SERVICENOW.equals(targetSystem)) {
            throw new IllegalArgumentException(
                    "Unknown target system: " + targetSystem
                            + ". Must be one of: " + TARGET_ACTIVE_DIRECTORY + ", " + TARGET_SERVICENOW);
        }
    }
}
