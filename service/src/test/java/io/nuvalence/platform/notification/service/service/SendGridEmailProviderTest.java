package io.nuvalence.platform.notification.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import io.nuvalence.platform.notification.service.exception.UnprocessableNotificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

@ExtendWith(MockitoExtension.class)
class SendGridEmailProviderTest {

    @Mock private SendGrid sendGrid;

    @Mock private Response response;

    private SendGridEmailProvider emailProvider;

    private String to = "test@example.com";

    private String subject = "Subject";

    private String body = "Body";

    @BeforeEach
    void setUp() throws IOException {
        emailProvider = new SendGridEmailProvider(sendGrid);
    }

    @Test
    void testSendEmail() throws IOException {
        when(sendGrid.api(any(Request.class))).thenReturn(response);
        emailProvider.sendEmail(to, subject, body);

        verify(sendGrid).api(any(Request.class));
    }

    @Test
    void testSendEmail_BadRequest() throws IOException {
        IOException exception = new IOException("Request returned status Code 401Body: {}");
        when(sendGrid.api(any())).thenThrow(exception);

        UnprocessableNotificationException actualException =
                assertThrows(
                        UnprocessableNotificationException.class,
                        () -> emailProvider.sendEmail(to, subject, body));

        assertEquals(
                String.format(
                        "Bad request response obtained from SendGrid with code %d, could not send"
                                + " email to %s",
                        401, to),
                actualException.getMessage());
    }

    @Test
    void testSendEmail_IoExceptionHandling() throws IOException {
        IOException expectedException = new IOException("Expected exception");
        when(sendGrid.api(any())).thenThrow(expectedException);

        IOException actualException =
                assertThrows(IOException.class, () -> emailProvider.sendEmail(to, subject, body));
        assertEquals(expectedException, actualException);
    }

    @Test
    void testSendEmail_UnexpectedException() throws IOException {
        RuntimeException expectedException = new RuntimeException("Expected exception");
        when(sendGrid.api(any())).thenThrow(expectedException);

        RuntimeException actualException =
                assertThrows(
                        RuntimeException.class, () -> emailProvider.sendEmail(to, subject, body));
        assertEquals(expectedException, actualException);
    }
}
