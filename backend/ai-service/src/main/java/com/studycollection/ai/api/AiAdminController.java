package com.studycollection.ai.api;

import com.studycollection.ai.app.AiAnalysisService;
import com.studycollection.ai.app.AiCallAudit;
import com.studycollection.ai.app.AiCallAuditRepository;
import com.studycollection.ai.app.AiSettingsService;
import com.studycollection.ai.app.AnalysisAdvice;
import com.studycollection.ai.app.AnalysisMode;
import com.studycollection.common.api.ApiResponse;
import com.studycollection.common.security.AdminOnly;
import com.studycollection.common.security.AuthenticatedUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@AdminOnly
@RestController
@RequestMapping("/ai")
public class AiAdminController {
    private final AiSettingsService settingsService;
    private final AiAnalysisService analysisService;
    private final AiCallAuditRepository auditRepository;

    public AiAdminController(
            AiSettingsService settingsService,
            AiAnalysisService analysisService,
            AiCallAuditRepository auditRepository
    ) {
        this.settingsService = settingsService;
        this.analysisService = analysisService;
        this.auditRepository = auditRepository;
    }

    @GetMapping("/settings")
    public ApiResponse<AiSettingsResponse> settings() {
        return ApiResponse.success(AiSettingsResponse.from(
                settingsService.current(),
                settingsService.apiKeyConfigured()
        ));
    }

    @PutMapping("/settings")
    public ApiResponse<AiSettingsResponse> update(
            @RequestAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE) AuthenticatedUser currentUser,
            @RequestBody UpdateAiSettingsRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("AI 设置不能为空");
        }
        return ApiResponse.success(AiSettingsResponse.from(
                settingsService.update(request.endpoint(), request.modelName(), currentUser.userId()),
                settingsService.apiKeyConfigured()
        ));
    }

    @PostMapping("/settings/test")
    public ApiResponse<AiConnectionTestResponse> testConnection(
            @RequestAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE) AuthenticatedUser currentUser
    ) {
        AnalysisAdvice advice = analysisService.generate(
                AnalysisMode.ONLINE_MODEL,
                currentUser.userId(),
                "CONFIG_TEST",
                "这是连接测试。请只回复：连接成功。"
        );
        boolean success = "ONLINE_MODEL".equals(advice.source());
        return ApiResponse.success(new AiConnectionTestResponse(
                success,
                advice.source(),
                success ? "在线模型连接成功。" : advice.content()
        ));
    }

    @GetMapping("/audits")
    public ApiResponse<List<AiCallAudit>> audits(
            @RequestParam(name = "limit", defaultValue = "50") Integer limit
    ) {
        int normalizedLimit = limit == null ? 50 : limit;
        if (normalizedLimit < 1 || normalizedLimit > 100) {
            throw new IllegalArgumentException("审计记录数量必须在 1 到 100 之间");
        }
        return ApiResponse.success(auditRepository.findRecent(normalizedLimit));
    }
}
