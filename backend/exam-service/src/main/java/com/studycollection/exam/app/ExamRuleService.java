package com.studycollection.exam.app;

import com.studycollection.exam.api.CustomExamRequest;
import com.studycollection.exam.api.ExamRuleRequest;
import com.studycollection.exam.domain.ExamRule;
import com.studycollection.exam.domain.ExamRuleStatus;
import com.studycollection.exam.domain.ExamSession;
import com.studycollection.question.domain.Question;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class ExamRuleService {
    private final ExamRuleRepository repository;
    private final RuleBasedExamGenerator generator;
    private final ExamSessionService sessionService;
    private final Clock clock;

    @Autowired
    public ExamRuleService(
            ExamRuleRepository repository,
            RuleBasedExamGenerator generator,
            ExamSessionService sessionService
    ) {
        this(repository, generator, sessionService, Clock.systemUTC());
    }

    public ExamRuleService(
            ExamRuleRepository repository,
            RuleBasedExamGenerator generator,
            ExamSessionService sessionService,
            Clock clock
    ) {
        this.repository = repository;
        this.generator = generator;
        this.sessionService = sessionService;
        this.clock = clock;
    }

    public synchronized ExamRule create(Long administratorId, ExamRuleRequest request) {
        Instant now = clock.instant();
        return repository.save(ruleFrom(request, null, ExamRuleStatus.DRAFT, administratorId, now, now));
    }

    public synchronized ExamRule update(Long id, ExamRuleRequest request) {
        ExamRule current = repository.findById(id);
        return repository.save(ruleFrom(
                request,
                current.id(),
                ExamRuleStatus.DRAFT,
                current.createdBy(),
                current.createdAt(),
                clock.instant()
        ));
    }

    public synchronized ExamRule publish(Long id) {
        ExamRule current = repository.findById(id);
        generator.generate(current);
        return repository.save(copyWithStatus(current, ExamRuleStatus.PUBLISHED));
    }

    public synchronized ExamRule unpublish(Long id) {
        return repository.save(copyWithStatus(repository.findById(id), ExamRuleStatus.DRAFT));
    }

    public synchronized void delete(Long id) {
        repository.findById(id);
        repository.deleteById(id);
    }

    public List<ExamRule> listAll() {
        return repository.findAll();
    }

    public List<ExamRule> listPublished() {
        return repository.findAll().stream()
                .filter(rule -> rule.status() == ExamRuleStatus.PUBLISHED)
                .toList();
    }

    public synchronized ExamSession start(Long userId, Long ruleId) {
        ExamRule rule = repository.findById(ruleId);
        if (rule.status() != ExamRuleStatus.PUBLISHED) {
            throw new IllegalArgumentException("考试规则尚未发布");
        }
        List<Question> questions = generator.generate(rule);
        return sessionService.create(userId, new CustomExamRequest(
                rule.name(),
                rule.durationMinutes(),
                questions.stream().map(Question::id).toList()
        ));
    }

    private ExamRule ruleFrom(
            ExamRuleRequest request,
            Long id,
            ExamRuleStatus status,
            Long createdBy,
            Instant createdAt,
            Instant updatedAt
    ) {
        if (request == null) {
            throw new IllegalArgumentException("考试规则内容不能为空");
        }
        return new ExamRule(
                id,
                request.name(),
                request.description(),
                request.durationMinutes(),
                request.totalQuestions(),
                request.knowledgePoints(),
                request.typeQuotas(),
                request.difficultyQuotas(),
                status,
                createdBy,
                createdAt,
                updatedAt
        );
    }

    private ExamRule copyWithStatus(ExamRule rule, ExamRuleStatus status) {
        return new ExamRule(
                rule.id(),
                rule.name(),
                rule.description(),
                rule.durationMinutes(),
                rule.totalQuestions(),
                rule.knowledgePoints(),
                rule.typeQuotas(),
                rule.difficultyQuotas(),
                status,
                rule.createdBy(),
                rule.createdAt(),
                clock.instant()
        );
    }
}
