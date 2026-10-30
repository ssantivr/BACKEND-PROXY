package com.workshop.proxy.service;

import com.workshop.proxy.model.Product;
import com.workshop.proxy.proxy.MicroserviceProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// 2. Class that represents the real object
public class InventoryService implements MicroserviceProxy<Object> {

    public static final Set<String> OPERATIONS = Set.of("listProducts", "checkStock", "addStock", "reduceStock");
    public static final Set<String> READ_ONLY_OPERATIONS = Set.of("listProducts", "checkStock");

    private final Map<String, Product> products = new ConcurrentHashMap<>();

    public InventoryService() {
        System.out.println("Loading real service: Inventory");
        seed(new Product("P-1001", "Laptop", 15, 950.0));
        seed(new Product("P-1002", "Mouse", 120, 18.5));
        seed(new Product("P-1003", "Keyboard", 80, 35.0));
        seed(new Product("P-1004", "Monitor", 30, 210.0));
    }

    @Override
    public Object execute(String operation, Object... params) {
        SimulatedLatency.pause();
        return switch (operation) {
            case "listProducts" -> listProducts();
            case "checkStock" -> checkStock(params);
            case "addStock" -> addStock(params);
            case "reduceStock" -> reduceStock(params);
            default -> throw new UnknownOperationException("Unknown inventory operation: " + operation);
        };
    }

    private void seed(Product product) {
        products.put(product.id(), product);
    }

    private List<Product> listProducts() {
        return new ArrayList<>(products.values());
    }

    private Product checkStock(Object[] params) {
        ParamReader.requireCount(params, 1);
        return findProduct(ParamReader.text(params, 0));
    }

    private Product addStock(Object[] params) {
        ParamReader.requireCount(params, 2);
        String productId = ParamReader.text(params, 0);
        int quantity = ParamReader.positiveInt(params, 1);
        findProduct(productId);
        return products.compute(productId, (id, product) -> product.withStock(product.stock() + quantity));
    }

    private Product reduceStock(Object[] params) {
        ParamReader.requireCount(params, 2);
        String productId = ParamReader.text(params, 0);
        int quantity = ParamReader.positiveInt(params, 1);
        findProduct(productId);
        return products.compute(productId, (id, product) -> reduce(product, quantity));
    }

    private Product reduce(Product product, int quantity) {
        if (product.stock() < quantity) {
            throw new InsufficientStockException("Insufficient stock for " + product.id() + ": available " + product.stock());
        }
        return product.withStock(product.stock() - quantity);
    }

    private Product findProduct(String productId) {
        Product product = products.get(productId);
        if (product == null) {
            throw new NotFoundException("Product not found: " + productId);
        }
        return product;
    }
}
