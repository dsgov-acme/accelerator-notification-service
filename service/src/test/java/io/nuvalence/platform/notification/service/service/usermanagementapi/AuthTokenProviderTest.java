package io.nuvalence.platform.notification.service.service.usermanagementapi;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.nuvalence.auth.token.SelfSignedTokenGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class AuthTokenProviderTest {

    @Test
    void testGetToken() {
        AuthTokenProvider authTokenProvider = new AuthTokenProvider();

        SelfSignedTokenGenerator generator = Mockito.mock(SelfSignedTokenGenerator.class);
        Mockito.when(generator.generateToken(Mockito.any(String.class), Mockito.anyList()))
                .thenReturn("token");
        ReflectionTestUtils.setField(authTokenProvider, "generator", generator);

        assertNotNull(authTokenProvider.getToken());
        Mockito.verify(generator).generateToken(Mockito.any(String.class), Mockito.anyList());
    }
}
