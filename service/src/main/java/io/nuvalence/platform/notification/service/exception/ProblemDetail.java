package io.nuvalence.platform.notification.service.exception;

import lombok.Builder;
import lombok.Data;

/**
 * Problem detail object.
 */
@Builder
@Data
public class ProblemDetail {
    private int status;
    private String title;
    private String detail;
    private String traceId;
}
