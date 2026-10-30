package com.workshop.proxy.service;

public class InsufficientStockException extends ServiceException {

    public InsufficientStockException(String message) {
        super(409, message);
    }
}
