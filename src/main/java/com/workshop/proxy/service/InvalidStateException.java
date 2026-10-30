package com.workshop.proxy.service;

public class InvalidStateException extends ServiceException {

    public InvalidStateException(String message) {
        super(409, message);
    }
}
