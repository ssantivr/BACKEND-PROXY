package com.workshop.proxy.demo;

import com.workshop.proxy.proxy.LoggingProxy;
import com.workshop.proxy.proxy.MicroserviceProxy;
import com.workshop.proxy.repository.LogRepository;
import com.workshop.proxy.service.InventoryService;
import com.workshop.proxy.service.OrderService;
import com.workshop.proxy.service.PaymentService;
import com.workshop.proxy.service.ServiceException;

// 4. Client that only uses the interface
public class DemoMain {

    public static void main(String[] args) {
        LogRepository logRepository = new LogRepository();
        MicroserviceProxy<?> inventory = new LoggingProxy<Object>("inventory", InventoryService::new,
                InventoryService.OPERATIONS, InventoryService.READ_ONLY_OPERATIONS, logRepository);
        MicroserviceProxy<?> orders = new LoggingProxy<Object>("orders", OrderService::new,
                OrderService.OPERATIONS, OrderService.READ_ONLY_OPERATIONS, logRepository);
        MicroserviceProxy<?> payments = new LoggingProxy<Object>("payments", PaymentService::new,
                PaymentService.OPERATIONS, PaymentService.READ_ONLY_OPERATIONS, logRepository);
        System.out.println("Proxies created: no real service has been loaded yet");

        call("First call to inventory", inventory, "listProducts");
        call("First call to orders", orders, "listOrders");
        call("First call to payments", payments, "getPaymentStatus", "PAY-1001");
        call("Second call to inventory (real service is NOT loaded again)", inventory, "checkStock", "P-1001");
    }

    private static void call(String title, MicroserviceProxy<?> service, String operation, Object... params) {
        System.out.println("\n--- " + title + " ---");
        try {
            System.out.println("Result: " + service.execute(operation, params));
        } catch (ServiceException e) {
            System.out.println("Failed: " + e.getMessage());
        }
    }
}
