package com.studycollection.report.app;

import com.studycollection.ai.app.AiAnalysisService;
import com.studycollection.ai.app.AnalysisAdvice;
import com.studycollection.ai.app.AnalysisMode;
import com.studycollection.exam.app.LearningAttempt;
import com.studycollection.exam.app.LearningAttemptRepository;
import com.studycollection.question.app.QuestionRepository;
import com.studycollection.question.domain.Question;
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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LearningReportService {
    private final LearningAttemptRepository attemptRepository;
    private final LearningReportRepository reportRepository;
    private final WeakPointAnalyzer analyzer;
    private final AiAnalysisService aiAnalysisService;
    private final QuestionRepository questionRepository;
    private final Clock clock;

    @Autowired
    public LearningReportService(
            LearningAttemptRepository attemptRepository,
            LearningReportRepository reportRepository,
            WeakPointAnalyzer analyzer,
            AiAnalysisService aiAnalysisService,
            QuestionRepository questionRepository
    ) {
        this(
                attemptRepository,
                reportRepository,
                analyzer,
                aiAnalysisService,
                questionRepository,
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
        this.attemptRepository = attemptRepository;
        this.reportRepository = reportRepository;
        this.analyzer = analyzer;
        this.aiAnalysisService = aiAnalysisService;
        this.questionRepository = questionRepository;
        this.clock = clock;
    }

    @Transactional
    public LearningReportResponse generate(Long userId, AnalysisMode mode) {
        List<LearningAttempt> attempts = attemptRepository.findByUserId(userId);
        if (attempts.isEmpty()) {
            throw new IllegalArgumentException("暂无可用于分析的真实作答记录");
        }

        int answeredQuestionCount = (int) attempts.stream().filter(this::isAnswered).count();
        int gradedQuestionCount = (int) attempts.stream().filter(LearningAttempt::autoGraded).count();
        int correctQuestionCount = (int) attempts.stream()
                .filter(LearningAttempt::autoGraded)
                .filter(attempt -> Boolean.TRUE.equals(attempt.correct()))
                .count();
        LearningReport analysis = analyzer.analyze(attempts);
        AnalysisAdvice advice = aiAnalysisService.generate(mode, analysis.recommendation());
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
                strengtheningQuestions(analysis.weakestKnowledgePoint())
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

    private List<StrengtheningQuestion> strengtheningQuestions(String knowledgePoint) {
        return questionRepository.search(null, knowledgePoint, null, null).stream()
                .limit(5)
                .map(this::toStrengtheningQuestion)
                .toList();
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
