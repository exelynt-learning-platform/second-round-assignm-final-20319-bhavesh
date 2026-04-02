package com.shopease.service.impl;

import com.shopease.config.StripeConfig;
import com.shopease.dto.request.PaymentRequest;
import com.shopease.dto.response.PaymentResponse;
import com.shopease.entity.Order;
import com.shopease.entity.OrderItem;
import com.shopease.entity.OrderStatus;
import com.shopease.entity.PaymentStatus;
import com.shopease.exception.PaymentException;
import com.shopease.exception.ResourceNotFoundException;
import com.shopease.exception.UnauthorizedAccessException;
import com.shopease.repository.OrderRepository;
import com.shopease.service.PaymentService;
import com.shopease.util.SecurityUtils;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private static final String DEFAULT_SUCCESS_URL = "http://localhost:3000/order/success?session_id={CHECKOUT_SESSION_ID}";
    private static final String DEFAULT_CANCEL_URL = "http://localhost:3000/order/cancel";

    private final OrderRepository orderRepository;
    private final StripeConfig stripeConfig;
    private final SecurityUtils securityUtils;

    public PaymentServiceImpl(OrderRepository orderRepository,
                              StripeConfig stripeConfig,
                              SecurityUtils securityUtils) {
        this.orderRepository = orderRepository;
        this.stripeConfig = stripeConfig;
        this.securityUtils = securityUtils;
    }

    @Override
    @Transactional
    public PaymentResponse createCheckoutSession(PaymentRequest request) {
        Order order = findOrderById(request.getOrderId());
        validateOrderOwnership(order);
        validateOrderForPayment(order);

        try {
            List<SessionCreateParams.LineItem> lineItems = buildLineItems(order);

            String successUrl = request.getSuccessUrl() != null ? request.getSuccessUrl() : DEFAULT_SUCCESS_URL;
            String cancelUrl = request.getCancelUrl() != null ? request.getCancelUrl() : DEFAULT_CANCEL_URL;

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .setCustomerEmail(order.getUser().getEmail())
                    .addAllLineItem(lineItems)
                    .putMetadata("order_id", order.getId().toString())
                    .putMetadata("order_number", order.getOrderNumber())
                    .setShippingAddressCollection(
                            SessionCreateParams.ShippingAddressCollection.builder()
                                    .addAllowedCountry(SessionCreateParams.ShippingAddressCollection.AllowedCountry.US)
                                    .addAllowedCountry(SessionCreateParams.ShippingAddressCollection.AllowedCountry.CA)
                                    .build()
                    )
                    .build();

            Session session = Session.create(params);

            order.setStripeSessionId(session.getId());
            order.setPaymentStatus(PaymentStatus.PROCESSING);
            orderRepository.save(order);

            logger.info("Checkout session created for order {}: {}", order.getOrderNumber(), session.getId());
            
            return PaymentResponse.success(session.getId(), session.getUrl());

        } catch (StripeException e) {
            logger.error("Stripe error creating checkout session: {}", e.getMessage());
            throw new PaymentException("Failed to create checkout session: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void handleWebhook(String payload, String signature) {
        Event event;
        
        try {
            event = Webhook.constructEvent(payload, signature, stripeConfig.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            logger.error("Webhook signature verification failed: {}", e.getMessage());
            throw new PaymentException("Invalid webhook signature");
        }

        logger.info("Received Stripe webhook event: {}", event.getType());

        switch (event.getType()) {
            case "checkout.session.completed" -> handleCheckoutCompleted(event);
            case "checkout.session.expired" -> handleCheckoutExpired(event);
            case "payment_intent.payment_failed" -> handlePaymentFailed(event);
            default -> logger.debug("Unhandled event type: {}", event.getType());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentStatus(Long orderId) {
        Order order = findOrderById(orderId);
        validateOrderOwnership(order);

        return PaymentResponse.builder()
                .paymentIntentId(order.getStripePaymentIntentId())
                .sessionId(order.getStripeSessionId())
                .status(order.getPaymentStatus())
                .message(getPaymentStatusMessage(order.getPaymentStatus()))
                .build();
    }

    @Override
    @Transactional
    public PaymentResponse confirmPayment(String sessionId) {
        Order order = orderRepository.findByStripeSessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "sessionId", sessionId));

        try {
            Session session = Session.retrieve(sessionId);

            if ("complete".equals(session.getStatus())) {
                order.setPaymentStatus(PaymentStatus.COMPLETED);
                order.setOrderStatus(OrderStatus.CONFIRMED);
                order.setStripePaymentIntentId(session.getPaymentIntent());
                orderRepository.save(order);
                
                logger.info("Payment confirmed for order {}", order.getOrderNumber());
                return PaymentResponse.completed(session.getPaymentIntent());
            } else {
                return PaymentResponse.builder()
                        .sessionId(sessionId)
                        .status(order.getPaymentStatus())
                        .message("Payment session status: " + session.getStatus())
                        .build();
            }
        } catch (StripeException e) {
            logger.error("Error confirming payment: {}", e.getMessage());
            throw new PaymentException("Failed to confirm payment: " + e.getMessage(), e);
        }
    }

    private void handleCheckoutCompleted(Event event) {
        Session session = (Session) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new PaymentException("Failed to deserialize checkout session"));

        String orderId = session.getMetadata().get("order_id");
        
        orderRepository.findById(Long.parseLong(orderId)).ifPresent(order -> {
            order.setPaymentStatus(PaymentStatus.COMPLETED);
            order.setOrderStatus(OrderStatus.CONFIRMED);
            order.setStripePaymentIntentId(session.getPaymentIntent());
            orderRepository.save(order);
            logger.info("Order {} payment completed via webhook", order.getOrderNumber());
        });
    }

    private void handleCheckoutExpired(Event event) {
        Session session = (Session) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new PaymentException("Failed to deserialize checkout session"));

        String orderId = session.getMetadata().get("order_id");
        
        orderRepository.findById(Long.parseLong(orderId)).ifPresent(order -> {
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);
            logger.info("Order {} checkout session expired", order.getOrderNumber());
        });
    }

    private void handlePaymentFailed(Event event) {
        logger.warn("Payment failed event received: {}", event.getId());
    }

    private Order findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
    }

    private void validateOrderOwnership(Order order) {
        if (!securityUtils.canAccessResource(order.getUser().getId())) {
            throw new UnauthorizedAccessException("order", order.getId());
        }
    }

    private void validateOrderForPayment(Order order) {
        if (order.getPaymentStatus() == PaymentStatus.COMPLETED) {
            throw new PaymentException("Order has already been paid");
        }
        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new PaymentException("Cannot process payment for cancelled order");
        }
    }

    private List<SessionCreateParams.LineItem> buildLineItems(Order order) {
        List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();

        for (OrderItem item : order.getItems()) {
            lineItems.add(
                    SessionCreateParams.LineItem.builder()
                            .setQuantity((long) item.getQuantity())
                            .setPriceData(
                                    SessionCreateParams.LineItem.PriceData.builder()
                                            .setCurrency("usd")
                                            .setUnitAmount(item.getUnitPrice()
                                                    .multiply(BigDecimal.valueOf(100))
                                                    .longValue())
                                            .setProductData(
                                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                            .setName(item.getProductName())
                                                            .build()
                                            )
                                            .build()
                            )
                            .build()
            );
        }

        if (order.getShippingCost().compareTo(BigDecimal.ZERO) > 0) {
            lineItems.add(
                    SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(
                                    SessionCreateParams.LineItem.PriceData.builder()
                                            .setCurrency("usd")
                                            .setUnitAmount(order.getShippingCost()
                                                    .multiply(BigDecimal.valueOf(100))
                                                    .longValue())
                                            .setProductData(
                                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                            .setName("Shipping")
                                                            .build()
                                            )
                                            .build()
                            )
                            .build()
            );
        }

        if (order.getTax().compareTo(BigDecimal.ZERO) > 0) {
            lineItems.add(
                    SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(
                                    SessionCreateParams.LineItem.PriceData.builder()
                                            .setCurrency("usd")
                                            .setUnitAmount(order.getTax()
                                                    .multiply(BigDecimal.valueOf(100))
                                                    .longValue())
                                            .setProductData(
                                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                            .setName("Tax")
                                                            .build()
                                            )
                                            .build()
                            )
                            .build()
            );
        }

        return lineItems;
    }

    private String getPaymentStatusMessage(PaymentStatus status) {
        return switch (status) {
            case PENDING -> "Payment is pending";
            case PROCESSING -> "Payment is being processed";
            case COMPLETED -> "Payment completed successfully";
            case FAILED -> "Payment failed";
            case REFUNDED -> "Payment has been refunded";
            case CANCELLED -> "Payment was cancelled";
        };
    }
}
