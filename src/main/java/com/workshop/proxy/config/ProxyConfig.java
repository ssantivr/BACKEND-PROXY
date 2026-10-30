package com.workshop.proxy.config;

import com.workshop.proxy.proxy.LoggingProxy;
import com.workshop.proxy.proxy.MicroserviceProxy;
import com.workshop.proxy.repository.LogRepository;
import com.workshop.proxy.service.InventoryService;
import com.workshop.proxy.service.OrderService;
import com.workshop.proxy.service.PaymentService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Each real service is only a Supplier here: it is created on its first call (lazy loading)
@Configuration
public class ProxyConfig {

    @Bean
    public MicroserviceProxy<Object> inventoryProxy(LogRepository logRepository) {
        return new LoggingProxy<>("inventory", InventoryService::new,
                InventoryService.OPERATIONS, InventoryService.READ_ONLY_OPERATIONS, logRepository);
    }

    @Bean
    public MicroserviceProxy<Object> ordersProxy(LogRepository logRepository) {
        return new LoggingProxy<>("orders", OrderService::new,
                OrderService.OPERATIONS, OrderService.READ_ONLY_OPERATIONS, logRepository);
    }

    @Bean
    public MicroserviceProxy<Object> paymentsProxy(LogRepository logRepository) {
        return new LoggingProxy<>("payments", PaymentService::new,
                PaymentService.OPERATIONS, PaymentService.READ_ONLY_OPERATIONS, logRepository);
    }
}
