package com.shopease.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
public class PaymentException extends RuntimeException {

    private final String errorCode;
    private final String declineCode;

    public PaymentException(String message) {
        super(message);
        this.errorCode = null;
        this.declineCode = null;
    }

    public PaymentException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.declineCode = null;
    }

    public PaymentException(String message, String errorCode, String declineCode) {
        super(message);
        this.errorCode = errorCode;
        this.declineCode = declineCode;
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.declineCode = null;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getDeclineCode() {
        return declineCode;
    }
}
