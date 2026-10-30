package com.workshop.proxy.service;

// Reads and validates the params received by the real services
final class ParamReader {

    private ParamReader() {
    }

    static void requireCount(Object[] params, int expected) {
        if (params.length != expected) {
            throw new InvalidParamsException("Expected " + expected + " params but received " + params.length);
        }
    }

    static String text(Object[] params, int index) {
        if (params[index] instanceof String value && !value.isBlank()) {
            return value;
        }
        throw new InvalidParamsException("Param " + index + " must be a non-blank text");
    }

    static int positiveInt(Object[] params, int index) {
        if (params[index] instanceof Integer value && value > 0) {
            return value;
        }
        throw new InvalidParamsException("Param " + index + " must be a positive integer");
    }

    static double positiveAmount(Object[] params, int index) {
        if (params[index] instanceof Number value && value.doubleValue() > 0) {
            return value.doubleValue();
        }
        throw new InvalidParamsException("Param " + index + " must be a positive number");
    }
}
