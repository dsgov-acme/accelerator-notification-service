package io.nuvalence.platform.notification.service.mapper;

import io.nuvalence.platform.notification.service.domain.EmailLayout;
import io.nuvalence.platform.notification.service.generated.models.EmailLayoutRequestModel;
import io.nuvalence.platform.notification.service.generated.models.EmailLayoutResponseModel;
import org.mapstruct.Mapper;

/**
 * Mapper for email layouts.
 */
@Mapper(componentModel = "spring")
public interface EmailLayoutMapper {

    /**
     * Map an email layout request model to an email layout.
     *
     * @param emailLayoutRequestModel email layout request model
     * @return email layout
     */
    EmailLayout emailLayoutRequestModelToEmailLayout(
            EmailLayoutRequestModel emailLayoutRequestModel);

    /**
     * Map an email layout to an email layout response model.
     *
     * @param emailLayout email layout
     * @return email layout response model
     */
    EmailLayoutResponseModel emailLayoutToEmailLayoutResponseModel(EmailLayout emailLayout);
}
