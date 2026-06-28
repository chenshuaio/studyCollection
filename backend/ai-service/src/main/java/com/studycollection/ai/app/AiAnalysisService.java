package com.studycollection.ai.app;

public class AiAnalysisService {
    private final OnlineModelClient onlineModelClient;

    public AiAnalysisService() {
        this(new HttpOnlineModelClient());
    }

    public AiAnalysisService(OnlineModelClient onlineModelClient) {
        this.onlineModelClient = onlineModelClient;
    }

    public AnalysisAdvice generate(AnalysisMode mode, String summary) {
        if (mode == AnalysisMode.ONLINE_MODEL) {
            try {
                String advice = onlineModelClient.generateAdvice(summary);
                if (advice != null && !advice.isBlank()) {
                    return new AnalysisAdvice("ONLINE_MODEL", advice);
                }
            } catch (RuntimeException ignored) {
                return fallbackAdvice(summary, "在线模型暂不可用，已自动切换为规则分析。");
            }
            return fallbackAdvice(summary, "在线模型返回为空，已自动切换为规则分析。");
        }
        return fallbackAdvice(summary, "规则分析建议：");
    }

    private AnalysisAdvice fallbackAdvice(String summary, String prefix) {
        return new AnalysisAdvice("RULES", prefix + "请针对薄弱项继续练习。" + summary);
    }
}
