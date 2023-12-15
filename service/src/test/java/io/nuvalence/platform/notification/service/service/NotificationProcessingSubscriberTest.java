package io.nuvalence.platform.notification.service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.type.PhoneNumber;
import io.nuvalence.platform.notification.service.domain.EmailFormat;
import io.nuvalence.platform.notification.service.domain.EmailFormatContent;
import io.nuvalence.platform.notification.service.domain.EmailLayout;
import io.nuvalence.platform.notification.service.domain.LocalizedStringTemplate;
import io.nuvalence.platform.notification.service.domain.LocalizedStringTemplateLanguage;
import io.nuvalence.platform.notification.service.domain.MessageTemplate;
import io.nuvalence.platform.notification.service.domain.SmsFormat;
import io.nuvalence.platform.notification.service.service.usermanagementapi.UserManagementClientService;
import io.nuvalence.platform.notification.usermanagent.client.ApiException;
import io.nuvalence.platform.notification.usermanagent.client.generated.models.UserDTO;
import io.nuvalence.platform.notification.usermanagent.client.generated.models.UserPreferenceDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotificationProcessingSubscriberTest {

    @Autowired private ObjectMapper objectMapper;

    @Autowired private EmailLayoutService emailLayoutService;

    @Autowired private TemplateService templateService;

    @Autowired private MessageService messageService;

    @Autowired
    @Qualifier("notificationProcessingSubscriber") private NotificationProcessingSubscriber service;

    @MockBean private UserManagementClientService userManagementClientService;

    @MockBean private SendGrid sendGrid;

    private MessageTemplate createdTemplate;

    private ListAppender<ILoggingEvent> sendGridLogWatcher;

    private ListAppender<ILoggingEvent> sendMessageLogWatcher;

    private ListAppender<ILoggingEvent> emailMessageProviderLogWatcher;

    @BeforeAll
    void setUp() {
        final String emailLayoutKeykey = "emailLayoutKey";
        List<String> inputs =
                new ArrayList<>() {
                    private static final long serialVersionUID = 4861793309100343408L;

                    {
                        add("greeting");
                        add("body");
                        add("footer");
                    }
                };
        EmailLayout emailLayout = new EmailLayout();
        emailLayout.setName("name");
        emailLayout.setDescription("description");
        emailLayout.setContent(
                "<html>\n"
                        + "  <head>\n"
                        + "    <title>Application Submission Confirmation</title>\n"
                        + "  </head>\n"
                        + "  <body>\n"
                        + "    <div id=\\\"greeting\\\">\n"
                        + "           <p>{{greeting}}</p>\n"
                        + "           </div>\n"
                        + "           <div id=\\\"body\\\">\n"
                        + "           <p>{{body}}</p>\n"
                        + "           </div>\n"
                        + "           <div id=\\\"footer\\\">\n"
                        + "           <p>{{footer}}</p>\n"
                        + "           </div>\n"
                        + "           </body>\n"
                        + "  </html>");
        emailLayout.setInputs(inputs);

        final String templateKey = "key";
        Map<String, String> templateParameters = new HashMap<>();
        templateParameters.put("transactionId", "String");
        templateParameters.put("name", "String");

        final LocalizedStringTemplateLanguage localizedSmsStringTemplateLanguage1 =
                LocalizedStringTemplateLanguage.builder()
                        .language("en")
                        .template(
                                "Your financial benefits application, {{transactionId}}, has been"
                                        + " approved")
                        .build();

        final LocalizedStringTemplateLanguage localizedSmsStringTemplateLanguage2 =
                LocalizedStringTemplateLanguage.builder()
                        .language("es")
                        .template(
                                "Su solicitud de beneficios financieros, {{transactionId}}, ha"
                                        + " sido aprobada.")
                        .build();

        LocalizedStringTemplateLanguage localizedGreetingStringTemplateLanguage1 =
                LocalizedStringTemplateLanguage.builder()
                        .language("en")
                        .template("Hi {{name}}")
                        .build();

        LocalizedStringTemplateLanguage localizedGreetingStringTemplateLanguage2 =
                LocalizedStringTemplateLanguage.builder()
                        .language("es")
                        .template("Hola {{name}}")
                        .build();

        LocalizedStringTemplate localizedGreetingStringTemplate =
                LocalizedStringTemplate.builder()
                        .localizedTemplateStrings(
                                List.of(
                                        localizedGreetingStringTemplateLanguage1,
                                        localizedGreetingStringTemplateLanguage2))
                        .build();
        localizedGreetingStringTemplateLanguage1.setLocalizedStringTemplate(
                localizedGreetingStringTemplate);
        localizedGreetingStringTemplateLanguage2.setLocalizedStringTemplate(
                localizedGreetingStringTemplate);

        final EmailFormatContent emailFormatGreeting1 =
                EmailFormatContent.builder()
                        .emailLayoutInput("greeting")
                        .localizedStringTemplate(localizedGreetingStringTemplate)
                        .build();

        LocalizedStringTemplateLanguage localizedContentStringTemplateLanguage1 =
                LocalizedStringTemplateLanguage.builder()
                        .language("en")
                        .template(
                                "Your financial benefits application, {{transactionId}}, has been"
                                        + " approved.")
                        .build();

        LocalizedStringTemplateLanguage localizedContentStringTemplateLanguage2 =
                LocalizedStringTemplateLanguage.builder()
                        .language("es")
                        .template(
                                "Su solicitud de beneficios financieros, {{transactionId}}, ha"
                                        + " sido aprobada.")
                        .build();

        LocalizedStringTemplate localizedContentStringTemplate =
                LocalizedStringTemplate.builder()
                        .localizedTemplateStrings(
                                List.of(
                                        localizedContentStringTemplateLanguage1,
                                        localizedContentStringTemplateLanguage2))
                        .build();
        localizedContentStringTemplateLanguage1.setLocalizedStringTemplate(
                localizedContentStringTemplate);
        localizedContentStringTemplateLanguage2.setLocalizedStringTemplate(
                localizedContentStringTemplate);

        final EmailFormatContent emailFormatContent1 =
                EmailFormatContent.builder()
                        .emailLayoutInput("body")
                        .localizedStringTemplate(localizedContentStringTemplate)
                        .build();

        LocalizedStringTemplateLanguage localizedFooterStringTemplateLanguage1 =
                LocalizedStringTemplateLanguage.builder()
                        .language("en")
                        .template("Best regards,\\n DSGov")
                        .build();

        LocalizedStringTemplateLanguage localizedFooterStringTemplateLanguage2 =
                LocalizedStringTemplateLanguage.builder()
                        .language("es")
                        .template("Cordialmente,\\n DSGov")
                        .build();

        LocalizedStringTemplate localizedFooterStringTemplate =
                LocalizedStringTemplate.builder()
                        .localizedTemplateStrings(
                                List.of(
                                        localizedFooterStringTemplateLanguage1,
                                        localizedFooterStringTemplateLanguage2))
                        .build();
        localizedFooterStringTemplateLanguage1.setLocalizedStringTemplate(
                localizedFooterStringTemplate);
        localizedFooterStringTemplateLanguage2.setLocalizedStringTemplate(
                localizedFooterStringTemplate);

        EmailFormatContent emailFormatFooter1 =
                EmailFormatContent.builder()
                        .emailLayoutInput("footer")
                        .localizedStringTemplate(localizedFooterStringTemplate)
                        .build();

        SmsFormat smsFormat =
                SmsFormat.builder()
                        .localizedStringTemplate(
                                LocalizedStringTemplate.builder()
                                        .localizedTemplateStrings(
                                                List.of(
                                                        localizedSmsStringTemplateLanguage1,
                                                        localizedSmsStringTemplateLanguage2))
                                        .build())
                        .build();
        localizedSmsStringTemplateLanguage1.setLocalizedStringTemplate(
                smsFormat.getLocalizedStringTemplate());
        localizedSmsStringTemplateLanguage2.setLocalizedStringTemplate(
                smsFormat.getLocalizedStringTemplate());

        LocalizedStringTemplateLanguage localizedSubjectStringTemplateLanguage1 =
                LocalizedStringTemplateLanguage.builder()
                        .language("en")
                        .template("Your Financial Benefits Application has been Approved")
                        .build();

        LocalizedStringTemplateLanguage localizedSubjectStringTemplateLanguage2 =
                LocalizedStringTemplateLanguage.builder()
                        .language("es")
                        .template("Su Solicitud de Beneficios Financieros ha sido Aprobada")
                        .build();

        LocalizedStringTemplateLanguage localizedSubjectStringTemplateLanguage3 =
                LocalizedStringTemplateLanguage.builder()
                        .language("it")
                        .template("La sua richiesta di benefici finanziari è stata approvata.")
                        .build();

        EmailFormat emailFormat =
                EmailFormat.builder()
                        .localizedSubjectStringTemplate(
                                LocalizedStringTemplate.builder()
                                        .localizedTemplateStrings(
                                                List.of(
                                                        localizedSubjectStringTemplateLanguage1,
                                                        localizedSubjectStringTemplateLanguage2,
                                                        localizedSubjectStringTemplateLanguage3))
                                        .build())
                        .emailFormatContents(
                                List.of(
                                        emailFormatGreeting1,
                                        emailFormatContent1,
                                        emailFormatFooter1))
                        .build();
        localizedSubjectStringTemplateLanguage1.setLocalizedStringTemplate(
                emailFormat.getLocalizedSubjectStringTemplate());
        localizedSubjectStringTemplateLanguage2.setLocalizedStringTemplate(
                emailFormat.getLocalizedSubjectStringTemplate());
        localizedSubjectStringTemplateLanguage3.setLocalizedStringTemplate(
                emailFormat.getLocalizedSubjectStringTemplate());
        emailFormatGreeting1.setEmailFormat(emailFormat);
        emailFormatContent1.setEmailFormat(emailFormat);
        emailFormatFooter1.setEmailFormat(emailFormat);

        EmailLayout createdEmailLayout =
                emailLayoutService.createEmailLayout(emailLayoutKeykey, emailLayout);

        MessageTemplate template =
                MessageTemplate.builder()
                        .key(templateKey)
                        .name("template name")
                        .description("template description")
                        .parameters(templateParameters)
                        .emailLayoutKey(createdEmailLayout.getKey())
                        .smsFormat(smsFormat)
                        .emailFormat(emailFormat)
                        .build();

        createdTemplate = templateService.createOrUpdateTemplate(templateKey, template);

        sendGridLogWatcher = new ListAppender<>();
        sendGridLogWatcher.start();
        Logger sendGridEmailEmailProviderLogger =
                ((Logger) LoggerFactory.getLogger(SendGridEmailProvider.class));
        sendGridEmailEmailProviderLogger.setLevel(Level.TRACE);
        sendGridEmailEmailProviderLogger.addAppender(sendGridLogWatcher);

        sendMessageLogWatcher = new ListAppender<>();
        sendMessageLogWatcher.start();
        Logger sendMessageLogger = ((Logger) LoggerFactory.getLogger(SendMessageService.class));
        sendMessageLogger.setLevel(Level.WARN);
        sendMessageLogger.addAppender(sendMessageLogWatcher);

        emailMessageProviderLogWatcher = new ListAppender<>();
        emailMessageProviderLogWatcher.start();
        Logger emailMessageProviderLogger =
                ((Logger) LoggerFactory.getLogger(EmailMessageProvider.class));
        emailMessageProviderLogger.setLevel(Level.WARN);
        emailMessageProviderLogger.addAppender(emailMessageProviderLogWatcher);
    }

    @BeforeEach
    void clearLogWatchers() {
        sendGridLogWatcher.list.clear();
        sendMessageLogWatcher.list.clear();
        emailMessageProviderLogWatcher.list.clear();
    }

    @Test
    void testHandleMessage_sms() throws JsonProcessingException, ApiException {
        UUID userId = UUID.randomUUID();
        BasicAcknowledgeablePubsubMessage ack =
                Mockito.mock(BasicAcknowledgeablePubsubMessage.class);
        Message<?> message =
                MessageBuilder.withPayload(generateJsonMessage(userId))
                        .setHeader(GcpPubSubHeaders.ORIGINAL_MESSAGE, ack)
                        .build();

        Mockito.when(userManagementClientService.getUser(Mockito.any()))
                .thenReturn(createUser(userId, "en_US", "sms", false));

        MessageCreator messageCreator = mock(MessageCreator.class);

        try (MockedStatic<com.twilio.rest.api.v2010.account.Message> messageMock =
                mockStatic(com.twilio.rest.api.v2010.account.Message.class)) {
            messageMock
                    .when(
                            () ->
                                    com.twilio.rest.api.v2010.account.Message.creator(
                                            any(PhoneNumber.class),
                                            any(PhoneNumber.class),
                                            anyString()))
                    .thenReturn(messageCreator);
            service.handleMessage(message);
        }

        Mockito.verify(ack).ack();
    }

    @Test
    void testHandleMessage_email() throws IOException, ApiException {
        UUID userId = UUID.randomUUID();
        BasicAcknowledgeablePubsubMessage ack =
                Mockito.mock(BasicAcknowledgeablePubsubMessage.class);
        Message<?> message =
                MessageBuilder.withPayload(generateJsonMessage(userId))
                        .setHeader(GcpPubSubHeaders.ORIGINAL_MESSAGE, ack)
                        .build();
        Mockito.when(userManagementClientService.getUser(any()))
                .thenReturn(createUser(userId, "en", "email", false));

        Response response = new Response();
        Mockito.when(sendGrid.api(any())).thenReturn(response);

        service.handleMessage(message);

        assertEquals(1, sendGridLogWatcher.list.size());
        ILoggingEvent logEvent = sendGridLogWatcher.list.get(0);
        assertEquals("Email sent to {} with status code {}", logEvent.getMessage());

        Mockito.verify(ack).ack();
    }

    @Test
    void messageHandling_UserNotFound() throws IOException, ApiException {
        UUID userId = UUID.randomUUID();
        BasicAcknowledgeablePubsubMessage ack =
                Mockito.mock(BasicAcknowledgeablePubsubMessage.class);
        Message<?> message =
                MessageBuilder.withPayload(generateJsonMessage(userId))
                        .setHeader(GcpPubSubHeaders.ORIGINAL_MESSAGE, ack)
                        .build();

        Mockito.when(userManagementClientService.getUser(any())).thenReturn(Optional.empty());

        service.handleMessage(message);
        assertEquals(1, sendMessageLogWatcher.list.size());
        ILoggingEvent logEvent = sendMessageLogWatcher.list.get(0);
        assertEquals(
                String.format("Message could not be sent. User not found %s", userId),
                logEvent.getMessage());

        Mockito.verify(ack).ack();
    }

    private static Stream<Object[]> testData() {
        return Stream.of(
                new Object[] {
                    UUID.randomUUID(), null, null, false, "Communication preferences not found"
                },
                new Object[] {
                    UUID.randomUUID(),
                    null,
                    "carrierPigeon",
                    false,
                    "Preferred communication method not supported"
                },
                new Object[] {
                    UUID.randomUUID(),
                    null,
                    "carrierPigeon",
                    false,
                    "Preferred communication method not supported"
                });
    }

    @ParameterizedTest(name = "Test {index}: {4}")
    @MethodSource("testData")
    void messageHandling(
            UUID userId, String preferences, String method, boolean condition, String testName)
            throws IOException, ApiException {
        BasicAcknowledgeablePubsubMessage ack =
                Mockito.mock(BasicAcknowledgeablePubsubMessage.class);
        Message<?> message =
                MessageBuilder.withPayload(generateJsonMessage(userId))
                        .setHeader(GcpPubSubHeaders.ORIGINAL_MESSAGE, ack)
                        .build();

        Mockito.when(userManagementClientService.getUser(any()))
                .thenReturn(createUser(userId, preferences, method, condition));

        service.handleMessage(message);
        assertNotNull(sendMessageLogWatcher);
        ILoggingEvent logEvent = sendMessageLogWatcher.list.get(0);
        assertEquals(
                String.format("Message could not be sent. %s for user %s", testName, userId),
                logEvent.getMessage());
        Mockito.verify(ack).ack();
    }

    @Test
    void messageHandling_TemplateNotFound() throws IOException, ApiException {
        UUID userId = UUID.randomUUID();
        BasicAcknowledgeablePubsubMessage ack =
                Mockito.mock(BasicAcknowledgeablePubsubMessage.class);
        Message<?> message =
                MessageBuilder.withPayload(generateInvalidTemplateKeyJsonMessage(userId))
                        .setHeader(GcpPubSubHeaders.ORIGINAL_MESSAGE, ack)
                        .build();

        Mockito.when(userManagementClientService.getUser(any()))
                .thenReturn(createUser(userId, null, "email", false));

        service.handleMessage(message);
        assertEquals(1, sendMessageLogWatcher.list.size());
        ILoggingEvent logEvent = sendMessageLogWatcher.list.get(0);
        assertEquals(
                String.format(
                        "Message could not be sent. Template not found for template key invalid"),
                logEvent.getMessage());

        Mockito.verify(ack).ack();
    }

    @Test
    void messageHandling_EmailMessageTemplateNotFound() throws IOException, ApiException {
        UUID userId = UUID.randomUUID();
        BasicAcknowledgeablePubsubMessage ack =
                Mockito.mock(BasicAcknowledgeablePubsubMessage.class);
        Message<?> message =
                MessageBuilder.withPayload(generateInvalidTemplateKeyJsonMessage(userId))
                        .setHeader(GcpPubSubHeaders.ORIGINAL_MESSAGE, ack)
                        .build();

        Mockito.when(userManagementClientService.getUser(any()))
                .thenReturn(createUser(userId, null, "email", false));

        service.handleMessage(message);
        assertEquals(1, sendMessageLogWatcher.list.size());
        ILoggingEvent logEvent = sendMessageLogWatcher.list.get(0);
        assertEquals(
                String.format(
                        "Message could not be sent. Template not found for template key invalid"),
                logEvent.getMessage());

        Mockito.verify(ack).ack();
    }

    @Test
    void messageHandling_EmailSubjectNotFound() throws IOException, ApiException {
        UUID userId = UUID.randomUUID();
        BasicAcknowledgeablePubsubMessage ack =
                Mockito.mock(BasicAcknowledgeablePubsubMessage.class);
        Message<?> message =
                MessageBuilder.withPayload(generateJsonMessage(userId))
                        .setHeader(GcpPubSubHeaders.ORIGINAL_MESSAGE, ack)
                        .build();

        Mockito.when(userManagementClientService.getUser(any()))
                .thenReturn(createUser(userId, "fr", "email", false));

        service.handleMessage(message);
        assertEquals(1, emailMessageProviderLogWatcher.list.size());
        ILoggingEvent logEvent = emailMessageProviderLogWatcher.list.get(0);
        assertEquals(
                String.format(
                        "Could not send %s email to user %s, subject template not found",
                        createdTemplate.getKey(), userId),
                logEvent.getMessage());

        Mockito.verify(ack).ack();
    }

    @Test
    void messageHandling_EmailContentNotFound() throws IOException, ApiException {
        UUID userId = UUID.randomUUID();
        BasicAcknowledgeablePubsubMessage ack =
                Mockito.mock(BasicAcknowledgeablePubsubMessage.class);
        Message<?> message =
                MessageBuilder.withPayload(generateJsonMessage(userId))
                        .setHeader(GcpPubSubHeaders.ORIGINAL_MESSAGE, ack)
                        .build();

        Mockito.when(userManagementClientService.getUser(any()))
                .thenReturn(createUser(userId, "it", "email", false));

        service.handleMessage(message);
        assertEquals(1, emailMessageProviderLogWatcher.list.size());
        ILoggingEvent logEvent = emailMessageProviderLogWatcher.list.get(0);
        assertEquals(
                String.format(
                        "Could not send %s email to user %s, subject template not found",
                        createdTemplate.getKey(), userId),
                logEvent.getMessage());

        Mockito.verify(ack).ack();
    }

    private byte[] generateJsonMessage(UUID userId) throws JsonProcessingException {
        Map<String, String> parameters =
                Map.of(
                        "name", "Deibys Parra",
                        "transactionId", "38dh38");
        io.nuvalence.platform.notification.service.domain.Message message =
                io.nuvalence.platform.notification.service.domain.Message.builder()
                        .messageTemplateKey(createdTemplate.getKey())
                        .userId(userId.toString())
                        .status("DRAFT")
                        .parameters(parameters)
                        .build();
        messageService.save(message);
        return objectMapper.writeValueAsString(message).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] generateInvalidTemplateKeyJsonMessage(UUID userId)
            throws JsonProcessingException {
        Map<String, String> parameters =
                Map.of(
                        "name", "Deibys Parra",
                        "transactionId", "38dh38");
        io.nuvalence.platform.notification.service.domain.Message message =
                io.nuvalence.platform.notification.service.domain.Message.builder()
                        .messageTemplateKey("invalid")
                        .userId(userId.toString())
                        .status("DRAFT")
                        .parameters(parameters)
                        .build();
        return objectMapper.writeValueAsString(message).getBytes(StandardCharsets.UTF_8);
    }

    private Optional<UserDTO> createUser(
            UUID id,
            String preferredLanguage,
            String preferredCommunicationMethod,
            boolean nullUserPreferences) {
        UserPreferenceDTO userPreferences = null;
        if (!nullUserPreferences) {
            userPreferences = new UserPreferenceDTO();
            userPreferences.setPreferredLanguage(preferredLanguage);
            userPreferences.setPreferredCommunicationMethod(preferredCommunicationMethod);
        }

        UserDTO user = new UserDTO();
        user.setId(id);
        user.setEmail("test@nobody.com");
        user.setPhoneNumber("1234567890");
        user.setPreferences(userPreferences);

        return Optional.of(user);
    }
}
