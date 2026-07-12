package com.studycollection.report.app;

import com.studycollection.ai.app.AiAnalysisService;
import com.studycollection.ai.app.AnalysisAdvice;
import com.studycollection.ai.app.AnalysisMode;
import com.studycollection.exam.app.LearningAttempt;
import com.studycollection.exam.app.LearningAttemptRepository;
import com.studycollection.question.app.InMemoryQuestionFeedbackRepository;
import com.studycollection.question.app.QuestionFeedbackRepository;
import com.studycollection.question.app.QuestionRepository;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionBankScope;
import com.studycollection.question.domain.QuestionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LearningReportService {
    private final LearningAttemptRepository attemptRepository;
    private final LearningReportRepository reportRepository;
    private final WeakPointAnalyzer analyzer;
    private final AiAnalysisService aiAnalysisService;
    private final QuestionRepository questionRepository;
    private final QuestionFeedbackRepository feedbackRepository;
    private final Clock clock;

    @Autowired
    public LearningReportService(
            LearningAttemptRepository attemptRepository,
            LearningReportRepository reportRepository,
            WeakPointAnalyzer analyzer,
            AiAnalysisService aiAnalysisService,
            QuestionRepository questionRepository,
            QuestionFeedbackRepository feedbackRepository
    ) {
        this(
                attemptRepository,
                reportRepository,
                analyzer,
                aiAnalysisService,
                questionRepository,
                feedbackRepository,
                Clock.systemUTC()
        );
    }

    public LearningReportService(
            LearningAttemptRepository attemptRepository,
            LearningReportRepository reportRepository,
            WeakPointAnalyzer analyzer,
            AiAnalysisService aiAnalysisService,
            QuestionRepository questionRepository,
            Clock clock
    ) {
        this(
                attemptRepository,
                reportRepository,
                analyzer,
                aiAnalysisService,
                questionRepository,
                new InMemoryQuestionFeedbackRepository(),
                clock
        );
    }

    public LearningReportService(
            LearningAttemptRepository attemptRepository,
            LearningReportRepository reportRepository,
            WeakPointAnalyzer analyzer,
            AiAnalysisService aiAnalysisService,
            QuestionRepository questionRepository,
            QuestionFeedbackRepository feedbackRepository,
            Clock clock
    ) {
        this.attemptRepository = attemptRepository;
        this.reportRepository = reportRepository;
        this.analyzer = analyzer;
        this.aiAnalysisService = aiAnalysisService;
        this.questionRepository = questionRepository;
        this.feedbackRepository = feedbackRepository;
        this.clock = clock;
    }

    @Transactional
    public LearningReportResponse generate(Long userId, AnalysisMode mode) {
        return generate(userId, mode, RevisedQuestionPolicy.EXCLUDE_REVISED);
    }

    @Transactional
    public LearningReportResponse generate(
            Long userId,
            AnalysisMode mode,
            RevisedQuestionPolicy revisionPolicy
    ) {
        List<LearningAttempt> recordedAttempts = attemptRepository.findByUserId(userId);
        if (recordedAttempts.isEmpty()) {
            throw new IllegalArgumentException("暂无可用于分析的真实作答记录");
        }
        Set<Long> scoringAffectedQuestionIds = feedbackRepository.findScoringAffectedQuestionIds();
        int revisedAttemptCount = (int) recordedAttempts.stream()
                .filter(attempt -> scoringAffectedQuestionIds.contains(attempt.questionId()))
                .count();
        List<LearningAttempt> attempts = applyRevisionPolicy(
                recordedAttempts,
                scoringAffectedQuestionIds,
                revisionPolicy
        );
        if (attempts.isEmpty()) {
            throw new IllegalArgumentException("排除已修订题后暂无可用于分析的作答记录");
        }

        int answeredQuestionCount = (int) attempts.stream().filter(this::isAnswered).count();
        int gradedQuestionCount = (int) attempts.stream().filter(LearningAttempt::autoGraded).count();
        int correctQuestionCount = (int) attempts.stream()
                .filter(LearningAttempt::autoGraded)
                .filter(attempt -> Boolean.TRUE.equals(attempt.correct()))
                .count();
        LearningReport analysis = analyzer.analyze(attempts);
        AnalysisAdvice advice = aiAnalysisService.generate(
                mode,
                userId,
                "LEARNING_REPORT",
                analysis.recommendation()
        );
        LearningReportResponse report = new LearningReportResponse(
                null,
                clock.instant(),
                analysis.weakestKnowledgePoint(),
                analysis.recommendation(),
                advice.source(),
                advice.content(),
                answeredQuestionCount,
                gradedQuestionCount,
                correctQuestionCount,
                accuracy(gradedQuestionCount, correctQuestionCount),
                breakdown(attempts, LearningAttempt::knowledgePoint),
                breakdown(attempts, attempt -> attempt.questionType().name()),
                recentTrend(attempts),
                strengtheningQuestions(userId, analysis.weakestKnowledgePoint()),
                revisionPolicy.name(),
                revisedAttemptCount
        );
        return reportRepository.save(userId, report);
    }

    @Transactional(readOnly = true)
    public List<LearningReportResponse> history(Long userId) {
        return reportRepository.findByUserId(userId);
    }

    private List<PerformanceBreakdown> breakdown(
            List<LearningAttempt> attempts,
            Function<LearningAttempt, String> classifier
    ) {
        return attempts.stream()
                .collect(Collectors.groupingBy(classifier))
                .entrySet()
                .stream()
                .map(entry -> toBreakdown(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(PerformanceBreakdown::label))
                .toList();
    }

    private PerformanceBreakdown toBreakdown(String label, List<LearningAttempt> attempts) {
        int graded = (int) attempts.stream().filter(LearningAttempt::autoGraded).count();
        int correct = (int) attempts.stream()
                .filter(LearningAttempt::autoGraded)
                .filter(attempt -> Boolean.TRUE.equals(attempt.correct()))
                .count();
        int answered = (int) attempts.stream().filter(this::isAnswered).count();
        return new PerformanceBreakdown(label, answered, graded, correct, accuracy(graded, correct));
    }

    private List<TrendPoint> recentTrend(List<LearningAttempt> attempts) {
        Map<LocalDate, List<LearningAttempt>> byDate = attempts.stream()
                .collect(Collectors.groupingBy(
                        attempt -> attempt.attemptedAt().atZone(ZoneOffset.UTC).toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        List<TrendPoint> points = byDate.entrySet().stream()
                .map(entry -> {
                    int graded = (int) entry.getValue().stream().filter(LearningAttempt::autoGraded).count();
                    int correct = (int) entry.getValue().stream()
                            .filter(LearningAttempt::autoGraded)
                            .filter(attempt -> Boolean.TRUE.equals(attempt.correct()))
                            .count();
                    return new TrendPoint(entry.getKey(), graded, correct, accuracy(graded, correct));
                })
                .sorted(Comparator.comparing(TrendPoint::date))
                .toList();
        int fromIndex = Math.max(0, points.size() - 7);
        return new ArrayList<>(points.subList(fromIndex, points.size()));
    }

    private List<StrengtheningQuestion> strengtheningQuestions(Long userId, String knowledgePoint) {
        return questionRepository.searchAccessible(
                        userId,
                        QuestionBankScope.ALL,
                        null,
                        knowledgePoint,
                        null,
                        null
                ).stream()
                .limit(5)
                .map(this::toStrengtheningQuestion)
                .toList();
    }

    private List<LearningAttempt> applyRevisionPolicy(
            List<LearningAttempt> attempts,
            Set<Long> scoringAffectedQuestionIds,
            RevisedQuestionPolicy policy
    ) {
        if (policy == RevisedQuestionPolicy.EXCLUDE_REVISED) {
            return attempts.stream()
                    .filter(attempt -> !scoringAffectedQuestionIds.contains(attempt.questionId()))
                    .toList();
        }
        return attempts.stream()
                .map(attempt -> scoringAffectedQuestionIds.contains(attempt.questionId())
                        ? recalculate(attempt)
                        : attempt)
                .toList();
    }

    private LearningAttempt recalculate(LearningAttempt attempt) {
        Question question = questionRepository.findById(attempt.questionId());
        boolean autoGraded = isObjective(question.type());
        Boolean correct = autoGraded ? answersMatch(question, attempt.submittedAnswer()) : null;
        return new LearningAttempt(
                attempt.id(),
                attempt.userId(),
                attempt.activityType(),
                attempt.referenceId(),
                question.id(),
                question.title(),
                question.type(),
                question.difficulty(),
                question.knowledgePoint(),
                attempt.submittedAnswer(),
                autoGraded,
                correct,
                Boolean.TRUE.equals(correct) ? 10 : 0,
                attempt.attemptedAt()
        );
    }

    private boolean isObjective(QuestionType type) {
        return switch (type) {
            case SINGLE_CHOICE, MULTIPLE_CHOICE, TRUE_FALSE, FILL_BLANK -> true;
            case SHORT_ANSWER, PROGRAMMING -> false;
        };
    }

    private boolean answersMatch(Question question, String submittedAnswer) {
        if (submittedAnswer == null) {
            return false;
        }
        if (question.type() == QuestionType.MULTIPLE_CHOICE) {
            return normalizeMultipleChoiceAnswer(question.answer())
                    .equals(normalizeMultipleChoiceAnswer(submittedAnswer));
        }
        return question.answer().trim().equalsIgnoreCase(submittedAnswer.trim());
    }

    private String normalizeMultipleChoiceAnswer(String answer) {
        String compact = answer.toUpperCase(Locale.ROOT).replaceAll("[\\s,，、;；|/]+", "");
        if (!compact.matches("[A-Z]+")) {
            return "";
        }
        return compact.chars()
                .distinct()
                .sorted()
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private StrengtheningQuestion toStrengtheningQuestion(Question question) {
        return new StrengtheningQuestion(
                question.id(),
                question.title(),
                question.type().name(),
                question.difficulty().name(),
                question.knowledgePoint()
        );
    }

    private double accuracy(int gradedQuestionCount, int correctQuestionCount) {
        return gradedQuestionCount == 0 ? 0 : (double) correctQuestionCount / gradedQuestionCount;
    }

    private boolean isAnswered(LearningAttempt attempt) {
        return !attempt.submittedAnswer().isBlank();
    }
}
