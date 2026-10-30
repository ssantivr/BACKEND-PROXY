package com.workshop.proxy.metrics;

import com.workshop.proxy.model.LoadSummary;
import com.workshop.proxy.model.Order;
import com.workshop.proxy.model.Payment;
import com.workshop.proxy.proxy.MicroserviceProxy;
import com.workshop.proxy.service.ServiceException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

// 4. Client that only uses the interface
@Component
public class LoadSimulator {

    private static final int TOTAL_CALLS = 50;
    private static final List<String> PRODUCT_IDS = List.of("P-1001", "P-1002", "P-1003", "P-1004");
    private static final List<String> SEED_ORDER_IDS = List.of("O-1001", "O-1002", "O-1003");
    private static final List<String> SEED_PAYMENT_IDS = List.of("PAY-1001", "PAY-1002");

    private final MicroserviceProxy<?> inventory;
    private final MicroserviceProxy<?> orders;
    private final MicroserviceProxy<?> payments;
    private final List<String> openOrderIds = new ArrayList<>();
    private final List<String> approvedPaymentIds = new ArrayList<>();
    private int failedCalls;

    public LoadSimulator(@Qualifier("inventoryProxy") MicroserviceProxy<?> inventory,
                         @Qualifier("ordersProxy") MicroserviceProxy<?> orders,
                         @Qualifier("paymentsProxy") MicroserviceProxy<?> payments) {
        this.inventory = inventory;
        this.orders = orders;
        this.payments = payments;
    }

    public synchronized LoadSummary simulate() {
        failedCalls = 0;
        for (int i = 0; i < TOTAL_CALLS; i++) {
            callRandomService();
        }
        return new LoadSummary(TOTAL_CALLS, TOTAL_CALLS - failedCalls, failedCalls);
    }

    private void callRandomService() {
        switch (randomInt(0, 3)) {
            case 0 -> callInventory();
            case 1 -> callOrders();
            default -> callPayments();
        }
    }

    private void callInventory() {
        switch (randomInt(0, 4)) {
            case 0 -> call(inventory, "listProducts");
            case 1 -> call(inventory, "checkStock", randomFrom(PRODUCT_IDS));
            case 2 -> call(inventory, "addStock", randomFrom(PRODUCT_IDS), randomInt(1, 20));
            default -> call(inventory, "reduceStock", randomFrom(PRODUCT_IDS), randomInt(1, 5));
        }
    }

    private void callOrders() {
        switch (randomInt(0, 4)) {
            case 0 -> createOrder();
            case 1 -> call(orders, "getOrder", randomFrom(SEED_ORDER_IDS));
            case 2 -> call(orders, "listOrders");
            default -> cancelOrder();
        }
    }

    private void callPayments() {
        switch (randomInt(0, 3)) {
            case 0 -> processPayment();
            case 1 -> call(payments, "getPaymentStatus", randomFrom(SEED_PAYMENT_IDS));
            default -> refundPayment();
        }
    }

    private void createOrder() {
        Object result = call(orders, "createOrder", "Customer " + randomInt(1, 100), randomFrom(PRODUCT_IDS), randomInt(1, 5));
        if (result instanceof Order order) {
            openOrderIds.add(order.id());
        }
    }

    // Cancels only orders created here, so the call is always valid
    private void cancelOrder() {
        if (openOrderIds.isEmpty()) {
            createOrder();
        } else {
            call(orders, "cancelOrder", openOrderIds.remove(0));
        }
    }

    private void processPayment() {
        Object result = call(payments, "processPayment", randomFrom(SEED_ORDER_IDS), (double) randomInt(10, 500));
        if (result instanceof Payment payment) {
            approvedPaymentIds.add(payment.id());
        }
    }

    // Refunds only payments created here, so the call is always valid
    private void refundPayment() {
        if (approvedPaymentIds.isEmpty()) {
            processPayment();
        } else {
            call(payments, "refundPayment", approvedPaymentIds.remove(0));
        }
    }

    private Object call(MicroserviceProxy<?> service, String operation, Object... params) {
        try {
            return service.execute(operation, params);
        } catch (ServiceException e) {
            failedCalls++;
            return null;
        }
    }

    private String randomFrom(List<String> values) {
        return values.get(randomInt(0, values.size()));
    }

    private int randomInt(int min, int maxExclusive) {
        return ThreadLocalRandom.current().nextInt(min, maxExclusive);
    }
}
