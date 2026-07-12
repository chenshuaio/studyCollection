package com.studycollection.ai.app;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Repository
@Profile("!local-mysql")
public class InMemoryAiModelSettingsRepository implements AiModelSettingsRepository {
    private final AtomicReference<AiModelSettings> settings = new AtomicReference<>();

    @Override
    public Optional<AiModelSettings> find() {
        return Optional.ofNullable(settings.get());
    }

    @Override
    public AiModelSettings save(AiModelSettings nextSettings) {
        settings.set(nextSettings);
        return nextSettings;
    }
}
