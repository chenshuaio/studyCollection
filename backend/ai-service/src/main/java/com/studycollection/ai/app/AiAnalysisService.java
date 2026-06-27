package com.studycollection.ai.app;

public class AiAnalysisService {
    public AnalysisAdvice generate(AnalysisMode mode, String summary) {
        if (mode == AnalysisMode.ONLINE_MODEL) {
            return new AnalysisAdvice("ONLINE_MODEL", "在线模型建议：" + summary);
        }
        return new AnalysisAdvice("RULES", "规则分析建议：请针对薄弱项继续练习。" + summary);
    }
}
