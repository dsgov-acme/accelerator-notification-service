package io.nuvalence.platform.notification.service.controller;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.platform.notification.service.domain.MessageTemplate;
import io.nuvalence.platform.notification.service.generated.models.EmailFormatModel;
import io.nuvalence.platform.notification.service.generated.models.EmailLayoutRequestModel;
import io.nuvalence.platform.notification.service.generated.models.LocalizedTemplateModel;
import io.nuvalence.platform.notification.service.generated.models.TemplateRequestModel;
import io.nuvalence.platform.notification.service.generated.models.TemplateRequestModelSmsFormat;
import io.nuvalence.platform.notification.service.repository.MessageTemplateRepository;
import io.nuvalence.platform.notification.service.service.EmailLayoutService;
import io.nuvalence.platform.notification.service.service.TemplateService;
import io.nuvalence.platform.notification.service.utils.XmlUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminNotificationApiControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private MessageTemplateRepository templateRepository;
    @Autowired private ApplicationContext applicationContext;
    @Autowired private TemplateService templateServiceBean;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockBean private AuthorizationHandler authorizationHandler;

    @BeforeEach
    void setup() {
        when(authorizationHandler.isAllowed(any(), (Class<?>) any())).thenReturn(true);
        when(authorizationHandler.isAllowed(any(), (String) any())).thenReturn(true);
        when(authorizationHandler.isAllowedForInstance(any(), any())).thenReturn(true);
        when(authorizationHandler.getAuthFilter(any(), any())).thenReturn(element -> true);
    }

    @AfterEach
    void tearDown() {
        templateRepository.deleteAll();
    }

    @Test
    void testCreateEmailLayout() throws Exception {
        String emailLayoutKey = RandomStringUtils.randomAlphanumeric(10);
        reusableCreateEmailLayoutRequest(emailLayoutKey);
    }

    @Test
    void testCreateEmailLayoutKeyUniquenessConstraint() throws Exception {
        var adminImplementation =
                applicationContext.getBean(AdminNotificationApiDelegateImpl.class);
        var emailLayoutService = mock(EmailLayoutService.class);

        ReflectionTestUtils.setField(adminImplementation, "emailLayoutService", emailLayoutService);

        var exception = mock(DataIntegrityViolationException.class);
        when(exception.getRootCause())
                .thenReturn(new RuntimeException("key value violates unique constraint"));

        when(emailLayoutService.createEmailLayout(anyString(), any())).thenThrow(exception);

        createEmailLayoutBaseRequest(RandomStringUtils.randomAlphanumeric(10))
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.title")
                                .value("Case-insensitive key already exists for this type."));

        ReflectionTestUtils.setField(adminImplementation, "emailLayoutService", emailLayoutService);
    }

    @Test
    void testgetEmailLayoutByKey() throws Exception {
        String emailLayoutKey = RandomStringUtils.randomAlphanumeric(10);
        reusableCreateEmailLayoutRequest(emailLayoutKey);

        mockMvc.perform(get("/api/v1/admin/email-layout/{key}", emailLayoutKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(notNullValue())))
                .andExpect(jsonPath("$.key", is(emailLayoutKey)))
                .andReturn();
    }

    @Test
    void testGetEmailLayoutByKey_not_found() throws Exception {
        String emailLayoutKey = RandomStringUtils.randomAlphanumeric(5);

        mockMvc.perform(get("/api/v1/admin/email-layout/{key}", emailLayoutKey))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateTemplate() throws Exception {
        reusableCreateTemplateRequest(RandomStringUtils.randomAlphanumeric(10));
    }

    @Test
    void testCreateTemplateKeyUniquenessConstraint() throws Exception {

        var adminImplementation =
                applicationContext.getBean(AdminNotificationApiDelegateImpl.class);
        var templateService = mock(TemplateService.class);

        ReflectionTestUtils.setField(adminImplementation, "templateService", templateService);

        var exception = mock(DataIntegrityViolationException.class);
        when(exception.getRootCause())
                .thenReturn(new RuntimeException("key value violates unique constraint"));

        when(templateService.createOrUpdateTemplate(anyString(), any())).thenThrow(exception);

        createTemplateBaseRequest(RandomStringUtils.randomAlphanumeric(10))
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.title")
                                .value("Case-insensitive key already exists for this type."));

        ReflectionTestUtils.setField(adminImplementation, "templateService", templateServiceBean);
    }

    private ResultActions createTemplateBaseRequest(String templateKey) throws Exception {
        String emailLayoutKey = RandomStringUtils.randomAlphanumeric(10);
        reusableCreateEmailLayoutRequest(emailLayoutKey);

        Map<String, String> templateParameters =
                Map.of(
                        "parameter1", "Date",
                        "parameter2", "String",
                        "parameter3", "Number",
                        "parameter4", "DateTime");

        LocalizedTemplateModel emailFormatSubject =
                new LocalizedTemplateModel(
                        Map.of(
                                "en", "email-subject-english",
                                "es", "email-subject-spanish",
                                "ca", "email-subject-catalan",
                                "f1", "email-subject-finnish"));

        Map<String, LocalizedTemplateModel> emailFormatContentBody = new HashMap<>();
        emailFormatContentBody.put(
                "body",
                new LocalizedTemplateModel(
                        Map.of(
                                "en", "email-body-english",
                                "es", "email-body-spanish",
                                "ca", "email-body-catalan",
                                "f1", "email-body-finnish")));

        EmailFormatModel emailFormat =
                new EmailFormatModel(emailLayoutKey, emailFormatSubject, emailFormatContentBody);

        TemplateRequestModelSmsFormat smsFormat = new TemplateRequestModelSmsFormat();
        LocalizedTemplateModel smsFormatMessage =
                new LocalizedTemplateModel(
                        Map.of(
                                "en", "email-sms-english",
                                "es", "email-sms-spanish",
                                "ca", "email-sms-catalan",
                                "f1", "email-sms-finnish"));
        smsFormat.setMessage(smsFormatMessage);

        TemplateRequestModel templateRequestModel =
                new TemplateRequestModel(
                        "templateName",
                        "templateDescription",
                        templateParameters,
                        emailFormat,
                        smsFormat);

        return mockMvc.perform(
                put("/api/v1/admin/templates/{key}", templateKey)
                        .content(objectMapper.writeValueAsString(templateRequestModel))
                        .contentType(MediaType.APPLICATION_JSON));
    }

    private void reusableCreateTemplateRequest(String templateKey) throws Exception {
        createTemplateBaseRequest(templateKey)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(notNullValue())))
                .andExpect(jsonPath("$.key", is(templateKey)))
                .andReturn();
    }

    private ResultActions createEmailLayoutBaseRequest(String emailLayoutKey) throws Exception {
        EmailLayoutRequestModel emailLayoutRequestModel =
                new EmailLayoutRequestModel(
                        "name",
                        "description",
                        "content",
                        new java.util.ArrayList<>(List.of("inputs")));

        return mockMvc.perform(
                put("/api/v1/admin/email-layout/{key}", emailLayoutKey)
                        .content(objectMapper.writeValueAsString(emailLayoutRequestModel))
                        .contentType(MediaType.APPLICATION_JSON));
    }

    private void reusableCreateEmailLayoutRequest(String emailLayoutKey) throws Exception {
        createEmailLayoutBaseRequest(emailLayoutKey)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(notNullValue())))
                .andExpect(jsonPath("$.key", is(emailLayoutKey)))
                .andReturn();
    }

    @Test
    void testGetLocalizationDataForExistingLanguage() throws Exception {

        reusableCreateTemplateRequest(RandomStringUtils.randomAlphanumeric(10));

        String targetLanguage = "es";

        var responseString =
                mockMvc.perform(
                                get("/api/v1/admin/localization-data")
                                        .param("locale", targetLanguage))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        verifyCompleteXliffResponse(responseString, targetLanguage, "spanish", 1, false);
    }

    @Test
    void testGetLocalizationDataTemplateForNewLanguage() throws Exception {

        reusableCreateTemplateRequest(RandomStringUtils.randomAlphanumeric(10));

        String targetLanguage = "fr";

        var responseString =
                mockMvc.perform(
                                get("/api/v1/admin/localization-data")
                                        .param("locale", targetLanguage))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        verifyCompleteXliffResponse(responseString, targetLanguage, "french", 1, true);
    }

    @Test
    void testPutLocalizationDataSuccess() throws Exception {

        reusableCreateTemplateRequest(RandomStringUtils.randomAlphanumeric(10));
        reusableCreateTemplateRequest(RandomStringUtils.randomAlphanumeric(10));

        String targetLanguage = "fr";

        String content =
                StreamUtils.copyToString(
                        Thread.currentThread()
                                .getContextClassLoader()
                                .getResourceAsStream("xliff/xliffPutTester.xlf"),
                        StandardCharsets.UTF_8);

        List<MessageTemplate> allTemplates = templateRepository.findAll();
        content = content.replaceAll("TO-BE-CHANGED-1", allTemplates.get(0).getKey());
        content = content.replaceAll("TO-BE-CHANGED-2", allTemplates.get(1).getKey());

        var responseString =
                mockMvc.perform(
                                put("/api/v1/admin/localization-data")
                                        .contentType(MediaType.APPLICATION_XML)
                                        .content(content))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        verifyCompleteXliffResponse(responseString, targetLanguage, "french", 2, false);
    }

    @Test
    void testPutLocalizationBadData() throws Exception {

        mockMvc.perform(
                        put("/api/v1/admin/localization-data")
                                .contentType(MediaType.APPLICATION_XML)
                                .content(
                                        "<?xml version=\"1.0\""
                                                + " encoding=\"UTF-8\"?><root></root>"))
                .andExpect(status().isBadRequest());
    }

    private void verifyCompleteXliffResponse(
            String responseString,
            String targeLanguage,
            String extLangSuffix,
            int existingTemplates,
            boolean targetPresentButEmpty)
            throws Exception {

        // validate entire response data based on database existing templates
        XmlUtils.xmlValidateAndMinify(responseString);

        var xmlDoc = XmlUtils.getXmlDocument(responseString);

        var root = xmlDoc.getRootElement();
        assertEquals("xliff", root.getName());
        assertEquals("1.2", root.getAttributeValue("version"));

        var namespace = root.getNamespace();
        assertEquals("urn:oasis:names:tc:xliff:document:1.2", namespace.getURI());

        var file = root.getChild("file", namespace);
        assertEquals("notification-service", file.getAttributeValue("original"));
        assertEquals(targeLanguage, file.getAttributeValue("target-language"));

        var mainGroups = file.getChild("body", namespace).getChildren("group", namespace);
        assertEquals(existingTemplates, mainGroups.size());

        for (var group : mainGroups) {
            assert (!group.getAttributeValue("resname").isBlank());

            var messageFormats = group.getChildren("group", namespace);
            assertEquals(2, messageFormats.size());

            var smsFormat = messageFormats.get(0);
            assertEquals("sms", smsFormat.getAttributeValue("resname"));

            var smsMessage = smsFormat.getChild("trans-unit", namespace);
            assertEquals("message", smsMessage.getAttributeValue("resname"));
            assertEquals("email-sms-english", smsMessage.getChild("source", namespace).getText());
            assertEquals(
                    targetPresentButEmpty ? "" : "email-sms-" + extLangSuffix,
                    smsMessage.getChild("target", namespace).getText());

            var emailFormat = messageFormats.get(1);
            assertEquals("email", emailFormat.getAttributeValue("resname"));
            var emailSubject = emailFormat.getChild("trans-unit", namespace);
            assertEquals("subject", emailSubject.getAttributeValue("resname"));

            assertEquals(
                    "email-subject-english", emailSubject.getChild("source", namespace).getText());
            assertEquals(
                    targetPresentButEmpty ? "" : "email-subject-" + extLangSuffix,
                    emailSubject.getChild("target", namespace).getText());

            var emailContent = emailFormat.getChild("group", namespace);
            assertEquals("content", emailContent.getAttributeValue("resname"));

            var emailContentTranslation = emailContent.getChild("trans-unit", namespace);
            assertEquals("body", emailContentTranslation.getAttributeValue("resname"));

            assertEquals(
                    "email-body-english",
                    emailContentTranslation.getChild("source", namespace).getText());
            assertEquals(
                    targetPresentButEmpty ? "" : "email-body-" + extLangSuffix,
                    emailContentTranslation.getChild("target", namespace).getText());
        }
    }
}
