package adrianmikula.jakartamigration.api.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for payment link requests.
 */
@Data
@Builder
public class PaymentLinkResponse {
    private boolean success;
    private String productName;
    private String paymentLink;
    private String message;
}

