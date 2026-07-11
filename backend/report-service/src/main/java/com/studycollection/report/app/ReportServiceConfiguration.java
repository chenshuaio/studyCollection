package com.studycollection.report.app;

import com.studycollection.ai.app.AiAnalysisService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ReportServiceConfiguration {
    @Bean
    public AiAnalysisService aiAnalysisService() {
        return new AiAnalysisService();
    }
}
