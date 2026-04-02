package com.shopease.dto.response;

import com.shopease.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private String sessionId;
    private String sessionUrl;
    private String paymentIntentId;
    private PaymentStatus status;
    private String message;

    public static PaymentResponse success(String sessionId, String sessionUrl) {
        return PaymentResponse.builder()
                .sessionId(sessionId)
                .sessionUrl(sessionUrl)
                .status(PaymentStatus.PROCESSING)
                .message("Checkout session created successfully")
                .build();
    }

    public static PaymentResponse completed(String paymentIntentId) {
        return PaymentResponse.builder()
                .paymentIntentId(paymentIntentId)
                .status(PaymentStatus.COMPLETED)
                .message("Payment completed successfully")
                .build();
    }

    public static PaymentResponse failed(String message) {
        return PaymentResponse.builder()
                .status(PaymentStatus.FAILED)
                .message(message)
                .build();
    }
}
