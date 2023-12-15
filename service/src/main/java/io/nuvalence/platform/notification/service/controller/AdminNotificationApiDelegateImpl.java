package io.nuvalence.platform.notification.service.controller;

import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.platform.notification.service.domain.EmailLayout;
import io.nuvalence.platform.notification.service.domain.MessageTemplate;
import io.nuvalence.platform.notification.service.generated.controllers.AdminNotificationApiDelegate;
import io.nuvalence.platform.notification.service.generated.models.EmailLayoutPageDTO;
import io.nuvalence.platform.notification.service.generated.models.EmailLayoutRequestModel;
import io.nuvalence.platform.notification.service.generated.models.EmailLayoutResponseModel;
import io.nuvalence.platform.notification.service.generated.models.TemplatePageDTO;
import io.nuvalence.platform.notification.service.generated.models.TemplateRequestModel;
import io.nuvalence.platform.notification.service.generated.models.TemplateResponseModel;
import io.nuvalence.platform.notification.service.mapper.EmailLayoutMapper;
import io.nuvalence.platform.notification.service.mapper.PagingMetadataMapper;
import io.nuvalence.platform.notification.service.mapper.TemplateMapper;
import io.nuvalence.platform.notification.service.model.SearchEmailLayoutFilter;
import io.nuvalence.platform.notification.service.model.SearchTemplateFilter;
import io.nuvalence.platform.notification.service.service.EmailLayoutService;
import io.nuvalence.platform.notification.service.service.LocalizationService;
import io.nuvalence.platform.notification.service.service.TemplateService;
import jakarta.ws.rs.ForbiddenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Implementation of AdminNotificationApiDelegate.
 */
@RequiredArgsConstructor
@Slf4j
@Service
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
public class AdminNotificationApiDelegateImpl implements AdminNotificationApiDelegate {
    private static final String CREATE_PERMISSION = "create";
    private final EmailLayoutService emailLayoutService;
    private final EmailLayoutMapper emailLayoutMapper;
    private final TemplateService templateService;
    private final LocalizationService localizationService;
    private final TemplateMapper templateMapperImpl;
    private final PagingMetadataMapper pagingMetadataMapper;
    private final AuthorizationHandler authorizationHandler;

    @Override
    public ResponseEntity<EmailLayoutResponseModel> createEmailLayout(
            String key, EmailLayoutRequestModel emailLayoutRequestModel) {
        if (!authorizationHandler.isAllowed(CREATE_PERMISSION, EmailLayout.class)) {
            throw new ForbiddenException();
        }

        EmailLayout emailLayout =
                emailLayoutService.createEmailLayout(
                        key,
                        emailLayoutMapper.emailLayoutRequestModelToEmailLayout(
                                emailLayoutRequestModel));
        return ResponseEntity.ok(
                emailLayoutMapper.emailLayoutToEmailLayoutResponseModel(emailLayout));
    }

    @Override
    public ResponseEntity<EmailLayoutResponseModel> getEmailLayoutByKey(String key) {
        return emailLayoutService
                .getEmailLayout(key)
                .filter(
                        emailLayout ->
                                authorizationHandler.isAllowedForInstance("view", emailLayout))
                .map(
                        emailLayout ->
                                ResponseEntity.ok(
                                        emailLayoutMapper.emailLayoutToEmailLayoutResponseModel(
                                                emailLayout)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<EmailLayoutPageDTO> getEmailLayouts(
            Integer pageNumber, Integer pageSize, String sortOrder, String sortBy, String name) {
        SearchEmailLayoutFilter filter =
                SearchEmailLayoutFilter.builder()
                        .pageNumber(pageNumber)
                        .pageSize(pageSize)
                        .sortOrder(sortOrder)
                        .sortBy(sortBy)
                        .name(name)
                        .build();
        Page<EmailLayout> result = emailLayoutService.getEmailLayouts(filter);
        EmailLayoutPageDTO response =
                new EmailLayoutPageDTO(
                        result.getContent().stream()
                                .filter(
                                        emailLayout ->
                                                authorizationHandler.isAllowedForInstance(
                                                        "view", emailLayout))
                                .map(emailLayoutMapper::emailLayoutToEmailLayoutResponseModel)
                                .collect(Collectors.toList()),
                        pagingMetadataMapper.toPagingMetadata(result));
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<TemplateResponseModel> createTemplate(
            String key, TemplateRequestModel templateRequestModel) {
        if (!authorizationHandler.isAllowed(CREATE_PERMISSION, MessageTemplate.class)) {
            throw new ForbiddenException();
        }
        MessageTemplate template =
                templateService.createOrUpdateTemplate(
                        key,
                        templateMapperImpl.templateRequestModelToTemplate(templateRequestModel));
        return ResponseEntity.ok(templateMapperImpl.templateToTemplateResponseModel(template));
    }

    @Override
    public ResponseEntity<TemplateResponseModel> getTemplateByKey(String key) {
        return templateService
                .getTemplate(key)
                .filter(template -> authorizationHandler.isAllowedForInstance("view", template))
                .map(
                        template ->
                                ResponseEntity.ok(
                                        templateMapperImpl.templateToTemplateResponseModel(
                                                template)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<TemplatePageDTO> getTemplates(
            Integer pageNumber, Integer pageSize, String sortOrder, String sortBy, String name) {
        SearchTemplateFilter filter =
                SearchTemplateFilter.builder()
                        .pageNumber(pageNumber)
                        .pageSize(pageSize)
                        .sortOrder(sortOrder)
                        .sortBy(sortBy)
                        .name(name)
                        .build();
        Page<MessageTemplate> result = templateService.getTemplates(filter);
        TemplatePageDTO response =
                new TemplatePageDTO(
                        result.getContent().stream()
                                .filter(
                                        template ->
                                                authorizationHandler.isAllowedForInstance(
                                                        "view", template))
                                .map(templateMapperImpl::templateToTemplateResponseModel)
                                .collect(Collectors.toList()),
                        pagingMetadataMapper.toPagingMetadata(result));
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<String> getLocalizationData(String localeTag) {
        if (!authorizationHandler.isAllowed("view", MessageTemplate.class)) {
            throw new ForbiddenException();
        }

        return ResponseEntity.ok(localizationService.getLocalizationData(localeTag));
    }

    @Override
    public ResponseEntity<String> createOrUpdateLocalizationData(String xliffFileString) {
        if (!authorizationHandler.isAllowed(CREATE_PERMISSION, MessageTemplate.class)) {
            throw new ForbiddenException();
        }

        var localeAndTemplatesToPersist =
                localizationService.parseXliffToExistingMsgTemplates(xliffFileString);

        var locale = localeAndTemplatesToPersist.getFirst();
        var templatesToPersist = localeAndTemplatesToPersist.getSecond();

        templatesToPersist.forEach(
                template -> templateService.createOrUpdateTemplate(template.getKey(), template));

        return ResponseEntity.ok(localizationService.getLocalizationData(locale));
    }
}
