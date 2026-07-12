package com.studycollection.ai.app;

@FunctionalInterface
public interface OnlineModelClient {
    String generateAdvice(String summary);

    default String provider() {
        return "CUSTOM";
    }

    default String model() {
        return "";
    }
}
