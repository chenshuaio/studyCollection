package com.studycollection.exam.app;

import com.studycollection.exam.api.CustomExamRequest;
import com.studycollection.exam.domain.ExamAnswer;
import com.studycollection.exam.domain.ExamQuestionSnapshot;
import com.studycollection.exam.domain.ExamSession;
import com.studycollection.exam.domain.ExamStatus;
import com.studycollection.question.app.QuestionRepository;
import com.studycollection.question.domain.Question;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ExamSessionService {
    private final ExamSessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final PracticeStatsRepository statsRepository;
    private final LearningAttemptRepository attemptRepository;
    private final Clock clock;

    @Autowired
    public ExamSessionService(
            ExamSessionRepository sessionRepository,
            QuestionRepository questionRepository,
            PracticeStatsRepository statsRepository,
            LearningAttemptRepository attemptRepository
    ) {
        this(sessionRepository, questionRepository, statsRepository, attemptRepository, Clock.systemUTC());
    }

    public ExamSessionService(
            ExamSessionRepository sessionRepository,
            QuestionRepository questionRepository,
            PracticeStatsRepository statsRepository,
            Clock clock
    ) {
        this(sessionRepository, questionRepository, statsRepository, new InMemoryLearningAttemptRepository(), clock);
    }

    public ExamSessionService(
            ExamSessionRepository sessionRepository,
            QuestionRepository questionRepository,
            PracticeStatsRepository statsRepository,
            LearningAttemptRepository attemptRepository,
            Clock clock
    ) {
        this.sessionRepository = sessionRepository;
        this.questionRepository = questionRepository;
        this.statsRepository = statsRepository;
        this.attemptRepository = attemptRepository;
        this.clock = clock;
    }

    public synchronized ExamSession create(Long userId, CustomExamRequest request) {
        if (request == null || request.questionIds() == null || request.questionIds().isEmpty()) {
            throw new IllegalArgumentException("至少选择一道题目");
        }
        if (request.questionIds().stream().anyMatch(id -> id == null)) {
            throw new IllegalArgumentException("题目编号不能为空");
        }
        Set<Long> uniqueIds = new LinkedHashSet<>(request.questionIds());
        if (uniqueIds.size() != request.questionIds().size()) {
            throw new IllegalArgumentException("题目不能重复选择");
        }
        List<ExamQuestionSnapshot> snapshots = request.questionIds().stream()
                .map(questionRepository::findById)
                .map(question -> snapshot(question, request.questionIds().indexOf(question.id())))
                .toList();
        ExamSession session = ExamSession.start(
                userId,
                request.name(),
                request.durationMinutes(),
                clock.instant(),
                snapshots
        );
        return sessionRepository.create(session);
    }

    @Transactional
    public synchronized List<ExamSession> list(Long userId) {
        return sessionRepository.findByUserId(userId).stream()
                .map(session -> finalizeIfExpired(session, clock.instant()))
                .toList();
    }

    @Transactional
    public synchronized ExamSession get(Long userId, Long sessionId) {
        return finalizeIfExpired(ownedSession(userId, sessionId), clock.instant());
    }

    @Transactional
    public synchronized ExamSession saveAnswer(
            Long userId,
            Long sessionId,
            Long questionId,
            String submittedAnswer
    ) {
        ExamSession current = ownedSession(userId, sessionId);
        if (current.status() == ExamStatus.SUBMITTED) {
            return current;
        }
        Instant now = clock.instant();
        if (!now.isBefore(current.expiresAt())) {
            return complete(current, now);
        }
        return sessionRepository.save(current.saveAnswer(questionId, submittedAnswer));
    }

    @Transactional
    public synchronized ExamSession submit(Long userId, Long sessionId) {
        ExamSession current = ownedSession(userId, sessionId);
        if (current.status() == ExamStatus.SUBMITTED) {
            return current;
        }
        return complete(current, clock.instant());
    }

    private ExamSession ownedSession(Long userId, Long sessionId) {
        ExamSession session = sessionRepository.findById(sessionId);
        if (!session.userId().equals(userId)) {
            throw new IllegalArgumentException("考试记录不存在");
        }
        return session;
    }

    private ExamSession finalizeIfExpired(ExamSession session, Instant now) {
        if (session.status() == ExamStatus.IN_PROGRESS && !now.isBefore(session.expiresAt())) {
            return complete(session, now);
        }
        return session;
    }

    private ExamSession complete(ExamSession session, Instant completedAt) {
        ExamSession completed = sessionRepository.save(session.complete(completedAt));
        int answered = (int) completed.answers().values().stream()
                .map(ExamAnswer::submittedAnswer)
                .filter(answer -> answer != null && !answer.isBlank())
                .count();
        int graded = (int) completed.answers().values().stream()
                .filter(ExamAnswer::autoGraded)
                .count();
        int correct = (int) completed.answers().values().stream()
                .filter(ExamAnswer::autoGraded)
                .filter(answer -> Boolean.TRUE.equals(answer.correct()))
                .count();
        statsRepository.add(session.userId(), answered, graded, correct);
        attemptRepository.saveAll(completed.questions().stream().map(question -> {
            ExamAnswer answer = completed.answers().get(question.questionId());
            return new LearningAttempt(
                    null,
                    session.userId(),
                    LearningActivityType.EXAM,
                    String.valueOf(session.id()),
                    question.questionId(),
                    question.title(),
                    question.type(),
                    question.difficulty(),
                    question.knowledgePoint(),
                    answer.submittedAnswer(),
                    answer.autoGraded(),
                    answer.correct(),
                    answer.score(),
                    completedAt
            );
        }).toList());
        return completed;
    }

    private ExamQuestionSnapshot snapshot(Question question, int sortOrder) {
        if (question.answer() == null || question.answer().isBlank()) {
            throw new IllegalArgumentException("题目标准答案不能为空");
        }
        return new ExamQuestionSnapshot(
                question.id(),
                question.title(),
                question.type(),
                question.difficulty(),
                question.knowledgePoint(),
                question.answer(),
                question.analysis(),
                sortOrder
        );
    }
}
