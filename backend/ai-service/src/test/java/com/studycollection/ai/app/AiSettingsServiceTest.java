package com.studycollection.ai.app;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class AiSettingsServiceTest {
    @Test
    void storedSettingsOverrideEnvironmentWithoutExposingApiKey() {
        InMemoryAiModelSettingsRepository repository = new InMemoryAiModelSettingsRepository();
        repository.save(new AiModelSettings(
                "OPENAI_COMPATIBLE",
                "https://stored.example/v1/chat/completions",
                "stored-model",
                9L,
                Instant.parse("2026-07-12T08:00:00Z")
        ));
        AiSettingsService service = new AiSettingsService(
                repository,
                new AiEnvironmentConfig(
                        "https://env.example/v1/chat/completions",
                        "env-model",
                        "top-secret-key"
                )
        );

        AiModelSettings settings = service.current();

        assertThat(settings.endpoint()).isEqualTo("https://stored.example/v1/chat/completions");
        assertThat(settings.modelName()).isEqualTo("stored-model");
        assertThat(service.apiKeyConfigured()).isTrue();
        assertThat(settings.toString()).doesNotContain("top-secret-key");
    }

    @Test
    void environmentSuppliesDefaultsWhenNoSettingsWereSaved() {
        AiSettingsService service = new AiSettingsService(
                new InMemoryAiModelSettingsRepository(),
                new AiEnvironmentConfig(
                        "https://env.example/v1/chat/completions",
                        "env-model",
                        ""
                )
        );

        AiModelSettings settings = service.current();

        assertThat(settings.endpoint()).isEqualTo("https://env.example/v1/chat/completions");
        assertThat(settings.modelName()).isEqualTo("env-model");
        assertThat(service.apiKeyConfigured()).isFalse();
    }

    @Test
    void rejectsInvalidEndpointAndBlankModel() {
        AiSettingsService service = new AiSettingsService(
                new InMemoryAiModelSettingsRepository(),
                new AiEnvironmentConfig("", "", "secret")
        );

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.update("file:///tmp/model", "qwen-plus", 1L))
                .withMessageContaining("http");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.update("https://user@example.com/v1/chat/completions", "qwen-plus", 1L))
                .withMessageContaining("用户信息");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.update("https://example.com/v1/chat/completions", " ", 1L))
                .withMessageContaining("模型名称");
    }
}
