package com.shopease.controller;

import com.shopease.dto.request.PaymentRequest;
import com.shopease.dto.response.ApiResponse;
import com.shopease.dto.response.PaymentResponse;
import com.shopease.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Payment processing endpoints")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/checkout")
    @Operation(summary = "Create checkout session", 
               description = "Creates a Stripe checkout session for an order")
    public ResponseEntity<ApiResponse<PaymentResponse>> createCheckoutSession(
            @Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.createCheckoutSession(request);
        return ResponseEntity.ok(ApiResponse.success("Checkout session created", response));
    }

    @GetMapping("/status/{orderId}")
    @Operation(summary = "Get payment status", description = "Returns the payment status for an order")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentStatus(@PathVariable Long orderId) {
        PaymentResponse response = paymentService.getPaymentStatus(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/confirm")
    @Operation(summary = "Confirm payment", 
               description = "Confirms payment completion after Stripe checkout")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(@RequestParam String sessionId) {
        PaymentResponse response = paymentService.confirmPayment(sessionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/webhook")
    @Operation(summary = "Stripe webhook", description = "Handles Stripe webhook events")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok("Webhook processed");
    }
}
