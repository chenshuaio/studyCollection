package com.studycollection.ai.app;

@FunctionalInterface
public interface OnlineModelClient {
    String generateAdvice(String summary);
}
