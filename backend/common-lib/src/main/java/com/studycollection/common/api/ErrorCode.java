package com.studycollection.common.api;

public enum ErrorCode {
    UNAUTHORIZED("请先登录"),
    FORBIDDEN("没有权限"),
    VALIDATION_FAILED("请求参数不正确"),
    SERVICE_UNAVAILABLE("服务暂时不可用");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
