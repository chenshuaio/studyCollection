package com.studycollection.ai.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class AiServiceConfiguration {
    @Bean
    public AiEnvironmentConfig aiEnvironmentConfig(
            @Value("${STUDY_COLLECTION_AI_ENDPOINT:}") String endpoint,
            @Value("${STUDY_COLLECTION_AI_MODEL:}") String modelName,
            @Value("${STUDY_COLLECTION_AI_API_KEY:}") String apiKey
    ) {
        return new AiEnvironmentConfig(endpoint, modelName, apiKey);
    }

    @Bean
    public AiSettingsService aiSettingsService(
            AiModelSettingsRepository repository,
            AiEnvironmentConfig environmentConfig
    ) {
        return new AiSettingsService(repository, environmentConfig);
    }

    @Bean
    public OnlineModelClient onlineModelClient(
            AiSettingsService settingsService,
            ObjectMapper objectMapper
    ) {
        return new HttpOnlineModelClient(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                settingsService,
                objectMapper,
                Duration.ofSeconds(20)
        );
    }

    @Bean
    public AiAnalysisService aiAnalysisService(
            OnlineModelClient onlineModelClient,
            AiCallAuditRepository auditRepository
    ) {
        return new AiAnalysisService(onlineModelClient, auditRepository, Clock.systemUTC());
    }
}
