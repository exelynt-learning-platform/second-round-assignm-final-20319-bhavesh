package com.shopease.service;

import com.shopease.dto.request.PaymentRequest;
import com.shopease.dto.response.PaymentResponse;

public interface PaymentService {

    PaymentResponse createCheckoutSession(PaymentRequest request);

    void handleWebhook(String payload, String signature);

    PaymentResponse getPaymentStatus(Long orderId);

    PaymentResponse confirmPayment(String sessionId);
}
