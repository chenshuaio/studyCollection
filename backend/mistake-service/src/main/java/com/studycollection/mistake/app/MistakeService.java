package com.studycollection.mistake.app;

import com.studycollection.mistake.domain.MistakeFilter;
import com.studycollection.mistake.domain.MistakeRecord;
import com.studycollection.question.app.QuestionRepository;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class MistakeService {
    private final MistakeRepository mistakeRepository;
    private final QuestionRepository questionRepository;
    private final Clock clock;

    @Autowired
    public MistakeService(MistakeRepository mistakeRepository, QuestionRepository questionRepository) {
        this(mistakeRepository, questionRepository, Clock.systemUTC());
    }

    public MistakeService(
            MistakeRepository mistakeRepository,
            QuestionRepository questionRepository,
            Clock clock
    ) {
        this.mistakeRepository = mistakeRepository;
        this.questionRepository = questionRepository;
        this.clock = clock;
    }

    public MistakeRecord record(Long userId, Long questionId, String submittedAnswer, String sourceContext) {
        if (questionId == null) {
            throw new IllegalArgumentException("题目 ID 不能为空");
        }
        Question question = questionRepository.findAccessibleById(questionId, userId);
        var occurredAt = clock.instant();
        MistakeRecord occurrence = new MistakeRecord(
                userId,
                question.id(),
                question.title(),
                question.type(),
                question.knowledgePoint(),
                submittedAnswer == null ? "" : submittedAnswer,
                normalizeSource(sourceContext),
                "PENDING",
                1,
                occurredAt,
                occurredAt
        );
        return mistakeRepository.recordOccurrence(occurrence);
    }

    public MistakeRecord updateStatus(Long userId, Long questionId, String status) {
        if (!"PENDING".equals(status) && !"MASTERED".equals(status)) {
            throw new IllegalArgumentException("错题状态仅支持 PENDING 或 MASTERED");
        }
        return mistakeRepository.updateStatus(userId, questionId, status);
    }

    public List<MistakeRecord> list(
            Long userId,
            String knowledgePoint,
            QuestionType questionType,
            String status,
            LocalDate wrongFrom,
            LocalDate wrongTo
    ) {
        if (wrongFrom != null && wrongTo != null && wrongFrom.isAfter(wrongTo)) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }
        MistakeFilter filter = new MistakeFilter(
                knowledgePoint,
                questionType,
                status,
                wrongFrom == null ? null : wrongFrom.atStartOfDay(clock.getZone()).toInstant(),
                wrongTo == null ? null : wrongTo.plusDays(1).atStartOfDay(clock.getZone()).toInstant()
        );
        return mistakeRepository.findByUserId(userId, filter);
    }

    private String normalizeSource(String sourceContext) {
        if (sourceContext == null || sourceContext.isBlank()) {
            return "UNKNOWN";
        }
        String normalized = sourceContext.trim().toUpperCase();
        if (!List.of("PRACTICE", "EXAM", "MISTAKE_RETRY").contains(normalized)) {
            throw new IllegalArgumentException("错题来源不受支持");
        }
        return normalized;
    }
}
