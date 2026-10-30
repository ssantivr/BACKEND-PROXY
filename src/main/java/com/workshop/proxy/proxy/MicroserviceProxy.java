package com.workshop.proxy.proxy;

// 1. Interface that defines the functionality of the real object and the Proxy
public interface MicroserviceProxy<T> {

    T execute(String operation, Object... params);
}
