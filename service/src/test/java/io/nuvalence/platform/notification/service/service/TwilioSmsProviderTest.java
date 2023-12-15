package io.nuvalence.platform.notification.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.twilio.exception.ApiConnectionException;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.type.PhoneNumber;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

class TwilioSmsProviderTest {

    private final MessageCreator messageCreator = mock(MessageCreator.class);

    private TwilioSmsProvider smsProvider;
    private final String twilioPhoneNumber = "some_phone_number";

    @BeforeEach
    public void setUp() {
        smsProvider = new TwilioSmsProvider();
        smsProvider.setTwilioPhoneNumber(twilioPhoneNumber);
    }

    @Test
    void testSendSms() {

        String calledToNumber;
        String calledFromNumber;
        String calledMessage;

        try (MockedStatic<Message> messageMock = mockStatic(Message.class)) {
            messageMock
                    .when(
                            () ->
                                    Message.creator(
                                            any(PhoneNumber.class),
                                            any(PhoneNumber.class),
                                            anyString()))
                    .thenReturn(messageCreator);

            smsProvider.sendSms("to_number", "test_message");

            ArgumentCaptor<PhoneNumber> toNumberCaptor = ArgumentCaptor.forClass(PhoneNumber.class);
            ArgumentCaptor<PhoneNumber> fromNumberCaptor =
                    ArgumentCaptor.forClass(PhoneNumber.class);
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

            messageMock.verify(
                    () ->
                            Message.creator(
                                    toNumberCaptor.capture(),
                                    fromNumberCaptor.capture(),
                                    messageCaptor.capture()));

            calledToNumber = toNumberCaptor.getValue().toString();
            calledFromNumber = fromNumberCaptor.getValue().toString();
            calledMessage = messageCaptor.getValue();
        }

        Assertions.assertEquals("to_number", calledToNumber);
        Assertions.assertEquals(twilioPhoneNumber, calledFromNumber);
        Assertions.assertEquals("test_message", calledMessage);
    }

    @Test
    void testSendSms_ApiConnectionException() {
        try (MockedStatic<Message> messageMock = mockStatic(Message.class)) {
            messageMock
                    .when(
                            () ->
                                    Message.creator(
                                            any(PhoneNumber.class),
                                            any(PhoneNumber.class),
                                            anyString()))
                    .thenReturn(messageCreator);

            ApiConnectionException expectedException =
                    new ApiConnectionException("Expected exception");
            doThrow(expectedException).when(messageCreator).create();

            ApiConnectionException actualException =
                    assertThrows(
                            ApiConnectionException.class,
                            () -> smsProvider.sendSms("to_number", "test_message"));
            assertEquals(expectedException, actualException);
        }
    }

    @Test
    void testSendSms_ApiException() {
        try (MockedStatic<Message> messageMock = mockStatic(Message.class)) {
            messageMock
                    .when(
                            () ->
                                    Message.creator(
                                            any(PhoneNumber.class),
                                            any(PhoneNumber.class),
                                            anyString()))
                    .thenReturn(messageCreator);

            ApiException expectedException = new ApiException("Expected exception");
            doThrow(expectedException).when(messageCreator).create();

            ApiException actualException =
                    assertThrows(
                            ApiException.class,
                            () -> smsProvider.sendSms("to_number", "test_message"));
            assertEquals(expectedException, actualException);
        }
    }

    @Test
    void testSendSms_UnexpectedException() {
        try (MockedStatic<Message> messageMock = mockStatic(Message.class)) {
            messageMock
                    .when(
                            () ->
                                    Message.creator(
                                            any(PhoneNumber.class),
                                            any(PhoneNumber.class),
                                            anyString()))
                    .thenReturn(messageCreator);

            RuntimeException expectedException = new RuntimeException("Expected exception");
            doThrow(expectedException).when(messageCreator).create();

            RuntimeException actualException =
                    assertThrows(
                            RuntimeException.class,
                            () -> smsProvider.sendSms("to_number", "test_message"));
            assertEquals(expectedException, actualException);
        }
    }
}
