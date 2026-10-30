package com.workshop.proxy.service;

public class UnknownOperationException extends ServiceException {

    public UnknownOperationException(String message) {
        super(400, message);
    }
}
