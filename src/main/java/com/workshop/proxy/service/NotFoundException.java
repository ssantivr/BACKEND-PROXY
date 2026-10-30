package com.workshop.proxy.service;

public class NotFoundException extends ServiceException {

    public NotFoundException(String message) {
        super(404, message);
    }
}
