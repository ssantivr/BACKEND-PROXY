package com.workshop.proxy.model;

public record Payment(String id, String orderId, double amount, String status) {

    public Payment withStatus(String newStatus) {
        return new Payment(id, orderId, amount, newStatus);
    }
}
