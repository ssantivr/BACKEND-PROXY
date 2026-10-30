package com.workshop.proxy.service;

import com.workshop.proxy.model.Payment;
import com.workshop.proxy.proxy.MicroserviceProxy;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

// 2. Class that represents the real object
public class PaymentService implements MicroserviceProxy<Object> {

    public static final Set<String> OPERATIONS = Set.of("processPayment", "getPaymentStatus", "refundPayment");
    public static final Set<String> READ_ONLY_OPERATIONS = Set.of("getPaymentStatus");

    private static final double DEFAULT_FAILURE_RATE = 0.10;
    private static final String APPROVED = "APPROVED";
    private static final String REFUNDED = "REFUNDED";

    private final Map<String, Payment> payments = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1001);
    private final double failureRate;

    public PaymentService() {
        this(DEFAULT_FAILURE_RATE);
    }

    public PaymentService(double failureRate) {
        System.out.println("Loading real service: Payments");
        this.failureRate = failureRate;
        save("O-1001", 950.0);
        save("O-1002", 37.0);
    }

    @Override
    public Object execute(String operation, Object... params) {
        SimulatedLatency.pause();
        failRandomly();
        return switch (operation) {
            case "processPayment" -> processPayment(params);
            case "getPaymentStatus" -> getPaymentStatus(params);
            case "refundPayment" -> refundPayment(params);
            default -> throw new UnknownOperationException("Unknown payments operation: " + operation);
        };
    }

    // Intentional failure so the proxy can log ERROR
    private void failRandomly() {
        if (ThreadLocalRandom.current().nextDouble() < failureRate) {
            throw new PaymentGatewayException("Payment gateway timeout");
        }
    }

    private Payment save(String orderId, double amount) {
        Payment payment = new Payment("PAY-" + nextId.getAndIncrement(), orderId, amount, APPROVED);
        payments.put(payment.id(), payment);
        return payment;
    }

    private Payment processPayment(Object[] params) {
        ParamReader.requireCount(params, 2);
        return save(ParamReader.text(params, 0), ParamReader.positiveAmount(params, 1));
    }

    private Payment getPaymentStatus(Object[] params) {
        ParamReader.requireCount(params, 1);
        return findPayment(ParamReader.text(params, 0));
    }

    private Payment refundPayment(Object[] params) {
        ParamReader.requireCount(params, 1);
        String paymentId = ParamReader.text(params, 0);
        findPayment(paymentId);
        return payments.compute(paymentId, (id, payment) -> refund(payment));
    }

    private Payment refund(Payment payment) {
        if (REFUNDED.equals(payment.status())) {
            throw new InvalidStateException("Payment already refunded: " + payment.id());
        }
        return payment.withStatus(REFUNDED);
    }

    private Payment findPayment(String paymentId) {
        Payment payment = payments.get(paymentId);
        if (payment == null) {
            throw new NotFoundException("Payment not found: " + paymentId);
        }
        return payment;
    }
}
