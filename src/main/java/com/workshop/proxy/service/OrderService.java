package com.workshop.proxy.service;

import com.workshop.proxy.model.Order;
import com.workshop.proxy.proxy.MicroserviceProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// 2. Class that represents the real object
public class OrderService implements MicroserviceProxy<Object> {

    public static final Set<String> OPERATIONS = Set.of("createOrder", "getOrder", "listOrders", "cancelOrder");
    public static final Set<String> READ_ONLY_OPERATIONS = Set.of("getOrder", "listOrders");

    private static final String CREATED = "CREATED";
    private static final String CANCELLED = "CANCELLED";

    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1001);

    public OrderService() {
        System.out.println("Loading real service: Orders");
        save("Ana Torres", "P-1001", 1);
        save("Luis Gomez", "P-1002", 2);
        save("Maria Diaz", "P-1003", 1);
    }

    @Override
    public Object execute(String operation, Object... params) {
        SimulatedLatency.pause();
        return switch (operation) {
            case "createOrder" -> createOrder(params);
            case "getOrder" -> getOrder(params);
            case "listOrders" -> listOrders();
            case "cancelOrder" -> cancelOrder(params);
            default -> throw new UnknownOperationException("Unknown orders operation: " + operation);
        };
    }

    private Order save(String customer, String productId, int quantity) {
        Order order = new Order("O-" + nextId.getAndIncrement(), customer, productId, quantity, CREATED);
        orders.put(order.id(), order);
        return order;
    }

    private Order createOrder(Object[] params) {
        ParamReader.requireCount(params, 3);
        return save(ParamReader.text(params, 0), ParamReader.text(params, 1), ParamReader.positiveInt(params, 2));
    }

    private Order getOrder(Object[] params) {
        ParamReader.requireCount(params, 1);
        return findOrder(ParamReader.text(params, 0));
    }

    private List<Order> listOrders() {
        return new ArrayList<>(orders.values());
    }

    private Order cancelOrder(Object[] params) {
        ParamReader.requireCount(params, 1);
        String orderId = ParamReader.text(params, 0);
        findOrder(orderId);
        return orders.compute(orderId, (id, order) -> cancel(order));
    }

    private Order cancel(Order order) {
        if (CANCELLED.equals(order.status())) {
            throw new InvalidStateException("Order already cancelled: " + order.id());
        }
        return order.withStatus(CANCELLED);
    }

    private Order findOrder(String orderId) {
        Order order = orders.get(orderId);
        if (order == null) {
            throw new NotFoundException("Order not found: " + orderId);
        }
        return order;
    }
}
