package com.workshop.proxy.service;

public class InvalidParamsException extends ServiceException {

    public InvalidParamsException(String message) {
        super(400, message);
    }
}
