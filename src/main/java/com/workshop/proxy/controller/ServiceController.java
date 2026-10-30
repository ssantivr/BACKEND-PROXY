package com.workshop.proxy.controller;

import com.workshop.proxy.proxy.MicroserviceProxy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 4. Client that only uses the interface
@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final MicroserviceProxy<?> inventory;
    private final MicroserviceProxy<?> orders;
    private final MicroserviceProxy<?> payments;

    public ServiceController(@Qualifier("inventoryProxy") MicroserviceProxy<?> inventory,
                             @Qualifier("ordersProxy") MicroserviceProxy<?> orders,
                             @Qualifier("paymentsProxy") MicroserviceProxy<?> payments) {
        this.inventory = inventory;
        this.orders = orders;
        this.payments = payments;
    }

    @PostMapping("/inventory/{operation}")
    public Object callInventory(@PathVariable String operation, @RequestBody(required = false) List<Object> params) {
        return inventory.execute(operation, toArray(params));
    }

    @PostMapping("/orders/{operation}")
    public Object callOrders(@PathVariable String operation, @RequestBody(required = false) List<Object> params) {
        return orders.execute(operation, toArray(params));
    }

    @PostMapping("/payments/{operation}")
    public Object callPayments(@PathVariable String operation, @RequestBody(required = false) List<Object> params) {
        return payments.execute(operation, toArray(params));
    }

    private Object[] toArray(List<Object> params) {
        return params == null ? new Object[0] : params.toArray();
    }
}
