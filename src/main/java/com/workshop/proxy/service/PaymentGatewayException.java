package com.workshop.proxy.service;

public class PaymentGatewayException extends ServiceException {

    public PaymentGatewayException(String message) {
        super(504, message);
    }
}
