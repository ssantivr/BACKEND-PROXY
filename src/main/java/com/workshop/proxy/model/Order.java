package com.workshop.proxy.model;

public record Order(String id, String customer, String productId, int quantity, String status) {

    public Order withStatus(String newStatus) {
        return new Order(id, customer, productId, quantity, newStatus);
    }
}
