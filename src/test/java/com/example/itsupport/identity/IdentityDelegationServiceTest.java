package com.example.itsupport.identity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityDelegationServiceTest {

    @Mock
    private ReactiveOAuth2AuthorizedClientManager clientManager;

    private IdentityDelegationService service;

    @BeforeEach
    void setUp() {
        service = new IdentityDelegationService(clientManager);
    }

    @Test
    void getTokenForUser_activeDirectory_returnsToken() {
        var mockClient = mockAuthorizedClient("test-ad-token");
        when(clientManager.authorize(any(OAuth2AuthorizeRequest.class)))
                .thenReturn(Mono.just(mockClient));

        StepVerifier.create(service.getTokenForUser("user123", IdentityDelegationService.TARGET_ACTIVE_DIRECTORY))
                .expectNext("test-ad-token")
                .verifyComplete();
    }

    @Test
    void getTokenForUser_servicenow_returnsToken() {
        var mockClient = mockAuthorizedClient("test-snow-token");
        when(clientManager.authorize(any(OAuth2AuthorizeRequest.class)))
                .thenReturn(Mono.just(mockClient));

        StepVerifier.create(service.getTokenForUser("user456", IdentityDelegationService.TARGET_SERVICENOW))
                .expectNext("test-snow-token")
                .verifyComplete();
    }

    @Test
    void getServiceToken_returnsToken() {
        var mockClient = mockAuthorizedClient("service-token-123");
        when(clientManager.authorize(any(OAuth2AuthorizeRequest.class)))
                .thenReturn(Mono.just(mockClient));

        StepVerifier.create(service.getServiceToken(IdentityDelegationService.TARGET_ACTIVE_DIRECTORY))
                .expectNext("service-token-123")
                .verifyComplete();
    }

    @Test
    void getTokenForUser_unknownTarget_throwsIllegalArgument() {
        assertThatThrownBy(() -> service.getTokenForUser("user1", "unknown-system"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown target system");
    }

    @Test
    void getTokenForUser_emptyResponse_throwsException() {
        when(clientManager.authorize(any(OAuth2AuthorizeRequest.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(service.getTokenForUser("user1", IdentityDelegationService.TARGET_ACTIVE_DIRECTORY))
                .expectError(IdentityDelegationException.class)
                .verify();
    }

    private OAuth2AuthorizedClient mockAuthorizedClient(String tokenValue) {
        var accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                tokenValue,
                Instant.now(),
                Instant.now().plusSeconds(3600));

        var registration = org.springframework.security.oauth2.client.registration.ClientRegistration
                .withRegistrationId("test")
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS)
                .clientId("test-client")
                .tokenUri("http://localhost/token")
                .build();

        return new OAuth2AuthorizedClient(registration, "test-principal", accessToken);
    }
}
