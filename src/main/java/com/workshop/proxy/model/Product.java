package com.workshop.proxy.model;

public record Product(String id, String name, int stock, double price) {

    public Product withStock(int newStock) {
        return new Product(id, name, newStock, price);
    }
}
