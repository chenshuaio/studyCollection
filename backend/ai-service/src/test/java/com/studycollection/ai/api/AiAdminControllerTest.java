package com.studycollection.ai.api;

import com.studycollection.ai.app.AiAnalysisService;
import com.studycollection.ai.app.AiEnvironmentConfig;
import com.studycollection.ai.app.AiSettingsService;
import com.studycollection.ai.app.InMemoryAiCallAuditRepository;
import com.studycollection.ai.app.InMemoryAiModelSettingsRepository;
import com.studycollection.common.security.AdminOnly;
import com.studycollection.common.security.AuthenticatedUser;
import com.studycollection.common.security.Role;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class AiAdminControllerTest {
    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(1L, "admin", Role.ADMIN);

    @Test
    void adminCanReadUpdateTestAndAuditModelSettings() {
        InMemoryAiModelSettingsRepository settingsRepository = new InMemoryAiModelSettingsRepository();
        AiSettingsService settingsService = new AiSettingsService(
                settingsRepository,
                new AiEnvironmentConfig(
                        "https://env.example/v1/chat/completions",
                        "env-model",
                        "configured-secret"
                ),
                Clock.fixed(Instant.parse("2026-07-12T08:00:00Z"), ZoneOffset.UTC)
        );
        InMemoryAiCallAuditRepository audits = new InMemoryAiCallAuditRepository();
        AiAnalysisService analysisService = new AiAnalysisService(
                summary -> "连接成功",
                audits,
                Clock.fixed(Instant.parse("2026-07-12T08:01:00Z"), ZoneOffset.UTC)
        );
        AiAdminController controller = new AiAdminController(settingsService, analysisService, audits);

        AiSettingsResponse initial = controller.settings().data();
        AiSettingsResponse updated = controller.update(
                ADMIN,
                new UpdateAiSettingsRequest("https://api.example/v1/chat/completions", "qwen-plus")
        ).data();
        AiConnectionTestResponse tested = controller.testConnection(ADMIN).data();

        assertThat(initial.apiKeyConfigured()).isTrue();
        assertThat(updated.endpoint()).isEqualTo("https://api.example/v1/chat/completions");
        assertThat(updated.modelName()).isEqualTo("qwen-plus");
        assertThat(updated.toString()).doesNotContain("configured-secret");
        assertThat(tested.success()).isTrue();
        assertThat(tested.source()).isEqualTo("ONLINE_MODEL");
        assertThat(controller.audits(50).data()).singleElement().satisfies(audit -> {
            assertThat(audit.userId()).isEqualTo(ADMIN.userId());
            assertThat(audit.purpose()).isEqualTo("CONFIG_TEST");
        });
        assertThat(AiAdminController.class.isAnnotationPresent(AdminOnly.class)).isTrue();
    }

    @Test
    void rejectsAuditLimitsOutsideOneToOneHundred() {
        AiSettingsService settingsService = new AiSettingsService(
                new InMemoryAiModelSettingsRepository(),
                new AiEnvironmentConfig("", "", "")
        );
        InMemoryAiCallAuditRepository audits = new InMemoryAiCallAuditRepository();
        AiAdminController controller = new AiAdminController(
                settingsService,
                new AiAnalysisService(),
                audits
        );

        assertThatIllegalArgumentException().isThrownBy(() -> controller.audits(0));
        assertThatIllegalArgumentException().isThrownBy(() -> controller.audits(101));
    }

    @Test
    void auditLimitHasAnExplicitHttpParameterName() throws Exception {
        Method method = AiAdminController.class.getMethod("audits", Integer.class);
        RequestParam requestParam = method.getParameters()[0].getAnnotation(RequestParam.class);

        assertThat(requestParam).isNotNull();
        assertThat(requestParam.name()).isEqualTo("limit");
    }
}
