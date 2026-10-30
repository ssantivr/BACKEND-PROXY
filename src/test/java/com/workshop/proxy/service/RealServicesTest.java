package com.workshop.proxy.service;

import com.workshop.proxy.model.Order;
import com.workshop.proxy.model.Payment;
import com.workshop.proxy.model.Product;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RealServicesTest {

    @Test
    void inventoryReducesStockAndRejectsInsufficientStock() {
        InventoryService inventory = new InventoryService();

        Product product = (Product) inventory.execute("reduceStock", "P-1001", 5);

        assertEquals(10, product.stock());
        assertThrows(InsufficientStockException.class, () -> inventory.execute("reduceStock", "P-1001", 11));
        assertThrows(NotFoundException.class, () -> inventory.execute("checkStock", "P-9999"));
        assertThrows(InvalidParamsException.class, () -> inventory.execute("addStock", "P-1001", -1));
    }

    @Test
    void ordersCanBeCancelledOnlyOnce() {
        OrderService orders = new OrderService();

        Order order = (Order) orders.execute("createOrder", "Ana", "P-1001", 2);
        Order cancelled = (Order) orders.execute("cancelOrder", order.id());

        assertEquals("CANCELLED", cancelled.status());
        assertThrows(InvalidStateException.class, () -> orders.execute("cancelOrder", order.id()));
    }

    @Test
    void paymentsCanBeRefundedOnlyOnce() {
        PaymentService payments = new PaymentService(0);

        Payment payment = (Payment) payments.execute("processPayment", "O-1001", 100.5);
        Payment refunded = (Payment) payments.execute("refundPayment", payment.id());

        assertEquals("REFUNDED", refunded.status());
        assertThrows(InvalidStateException.class, () -> payments.execute("refundPayment", payment.id()));
    }

    @Test
    void paymentGatewayFailureComesFromRealService() {
        PaymentService payments = new PaymentService(1);

        assertThrows(PaymentGatewayException.class, () -> payments.execute("getPaymentStatus", "PAY-1001"));
    }
}
