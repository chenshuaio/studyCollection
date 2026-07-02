package com.studycollection.question.app;

import com.studycollection.question.domain.KnowledgePoint;

import java.util.List;

public interface KnowledgePointRepository {
    KnowledgePoint save(KnowledgePoint knowledgePoint);

    List<KnowledgePoint> findAll();

    KnowledgePoint disable(Long id);
}
