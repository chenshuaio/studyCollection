package com.studycollection.ai.app;

import java.util.List;

public interface AiCallAuditRepository {
    AiCallAudit save(AiCallAudit audit);

    List<AiCallAudit> findRecent(int limit);
}
