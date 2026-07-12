package com.studycollection.ai.app;

import java.util.Optional;

public interface AiModelSettingsRepository {
    Optional<AiModelSettings> find();

    AiModelSettings save(AiModelSettings settings);
}
