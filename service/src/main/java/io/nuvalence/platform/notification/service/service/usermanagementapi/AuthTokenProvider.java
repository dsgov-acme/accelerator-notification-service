package io.nuvalence.platform.notification.service.service.usermanagementapi;

import io.nuvalence.auth.token.SelfSignedTokenGenerator;
import io.nuvalence.auth.util.RsaKeyUtility;
import io.nuvalence.platform.notification.service.exception.TokenGeneratorCreationException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Simple class that houses and provides the service to service token used in user-manager
 * communication.
 */
@Slf4j
@Component
public class AuthTokenProvider {

    private SelfSignedTokenGenerator generator;
    private String token;

    private Instant tokenLastGenerated = Instant.MIN;

    @Value("${auth.token-client.self-signed.issuer}")
    private String tokenIssuer;

    @Value("${auth.token-client.self-signed.private-key}")
    private String privateKey;

    @PostConstruct
    private void createGenerator() {
        try {
            generator =
                    new SelfSignedTokenGenerator(
                            tokenIssuer,
                            Duration.ofMinutes(5),
                            RsaKeyUtility.getPrivateKeyFromString(privateKey));
        } catch (IOException e) {
            throw new TokenGeneratorCreationException("Error creating token generator", e);
        }
    }

    /**
     * Returns a cached JWT or generates a new one if there isn't an unexpired token in the cache.
     *
     * @return JWT as String
     */
    public String getToken() {
        if (token == null
                || tokenLastGenerated.isBefore(Instant.now().minus(Duration.ofMinutes(3)))) {
            generateToken();
        }

        return token;
    }

    private void generateToken() {
        if (generator == null) {
            throw new IllegalStateException(
                    "Token Generator not configured. This is likely because the environment has"
                            + " not been set.");
        }
        token =
                generator.generateToken(
                        "notification-service", List.of("um:reader", "um:application-client"));
        tokenLastGenerated = Instant.now();
    }
}
