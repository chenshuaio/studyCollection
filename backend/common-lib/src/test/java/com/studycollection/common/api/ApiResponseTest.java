package com.studycollection.common.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {
    @Test
    void successWrapsDataWithOkCode() {
        ApiResponse<String> response = ApiResponse.success("ready");

        assertThat(response.code()).isEqualTo("OK");
        assertThat(response.message()).isEqualTo("success");
        assertThat(response.data()).isEqualTo("ready");
    }

    @Test
    void failureUsesErrorCodeMessage() {
        ApiResponse<Void> response = ApiResponse.failure(ErrorCode.UNAUTHORIZED);

        assertThat(response.code()).isEqualTo("UNAUTHORIZED");
        assertThat(response.message()).isEqualTo("请先登录");
        assertThat(response.data()).isNull();
    }
}
