package com.studycollection.exam.api;

import com.studycollection.common.api.ApiResponse;
import com.studycollection.common.security.AuthenticatedUser;
import com.studycollection.exam.app.InMemoryLearningAttemptRepository;
import com.studycollection.exam.app.LearningActivityType;
import com.studycollection.exam.app.LearningAttempt;
import com.studycollection.exam.app.LearningAttemptRepository;
import com.studycollection.exam.app.PracticeStatsRepository;
import com.studycollection.exam.app.PracticeGenerator;
import com.studycollection.question.app.QuestionRepository;
import com.studycollection.question.domain.Question;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/practice")
public class PracticeController {
    private static final int POINTS_PER_QUESTION = 10;
    private final QuestionRepository questionRepository;
    private final PracticeStatsRepository statsRepository;
    private final PracticeGenerator practiceGenerator;
    private final LearningAttemptRepository attemptRepository;
    private final Clock clock;

    public PracticeController(QuestionRepository questionRepository, PracticeStatsRepository statsRepository) {
        this(
                questionRepository,
                statsRepository,
                new PracticeGenerator(questionRepository),
                new InMemoryLearningAttemptRepository(),
                Clock.systemUTC()
        );
    }

    public PracticeController(
            QuestionRepository questionRepository,
            PracticeStatsRepository statsRepository,
            PracticeGenerator practiceGenerator
    ) {
        this(
                questionRepository,
                statsRepository,
                practiceGenerator,
                new InMemoryLearningAttemptRepository(),
                Clock.systemUTC()
        );
    }

    @Autowired
    public PracticeController(
            QuestionRepository questionRepository,
            PracticeStatsRepository statsRepository,
            PracticeGenerator practiceGenerator,
            LearningAttemptRepository attemptRepository
    ) {
        this(questionRepository, statsRepository, practiceGenerator, attemptRepository, Clock.systemUTC());
    }

    public PracticeController(
            QuestionRepository questionRepository,
            PracticeStatsRepository statsRepository,
            PracticeGenerator practiceGenerator,
            LearningAttemptRepository attemptRepository,
            Clock clock
    ) {
        this.questionRepository = questionRepository;
        this.statsRepository = statsRepository;
        this.practiceGenerator = practiceGenerator;
        this.attemptRepository = attemptRepository;
        this.clock = clock;
    }

    @PostMapping("/generate")
    public ApiResponse<GeneratedPractice> generate(@RequestBody PracticeGenerateRequest request) {
        if (request == null || request.count() == null) {
            throw new IllegalArgumentException("练习题目数量不能为空");
        }
        List<GeneratedPracticeQuestion> questions = practiceGenerator.generate(
                        request.knowledgePoint(),
                        request.difficulty(),
                        request.type(),
                        request.count()
                ).stream()
                .map(question -> new GeneratedPracticeQuestion(
                        question.id(),
                        question.title(),
                        question.type(),
                        question.difficulty(),
                        question.knowledgePoint()
                ))
                .toList();
        return ApiResponse.success(new GeneratedPractice(
                request.count(),
                questions.size(),
                questions
        ));
    }

    @PostMapping("/submit")
    @Transactional
    public ApiResponse<PracticeResult> submit(
            @RequestAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE) AuthenticatedUser currentUser,
            @RequestBody PracticeSubmitRequest request
    ) {
        if (request == null || request.answers() == null || request.answers().isEmpty()) {
            throw new IllegalArgumentException("至少提交一道题目答案");
        }
        if (request.answers().stream().anyMatch(answer -> answer == null
                || answer.answer() == null
                || answer.answer().isBlank())) {
            throw new IllegalArgumentException("练习答案不能为空");
        }
        List<PracticeResultItem> items = request.answers().stream()
                .map(this::scoreAnswer)
                .toList();
        int score = items.stream().mapToInt(PracticeResultItem::score).sum();
        recordStats(currentUser.userId(), items);
        recordAttempts(currentUser.userId(), items);
        int totalScore = (int) items.stream().filter(PracticeResultItem::autoGraded).count()
                * POINTS_PER_QUESTION;
        return ApiResponse.success(new PracticeResult(score, totalScore, items));
    }

    @GetMapping("/stats")
    public ApiResponse<PracticeStats> stats(
            @RequestAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE) AuthenticatedUser currentUser
    ) {
        Long userId = currentUser.userId();
        return ApiResponse.success(statsRepository.findByUserId(userId));
    }

    private PracticeResultItem scoreAnswer(PracticeAnswer answer) {
        Question question = questionRepository.findById(answer.questionId());
        String correctAnswer = question.answer();
        String analysis = question.analysis();
        if (correctAnswer == null || correctAnswer.isBlank()) {
            throw new IllegalArgumentException("标准答案不能为空");
        }
        boolean autoGraded = isObjective(question);
        Boolean correct = autoGraded ? answersMatch(question, answer.answer()) : null;
        return new PracticeResultItem(
                answer.questionId(),
                answer.answer(),
                correctAnswer,
                autoGraded,
                correct,
                Boolean.TRUE.equals(correct) ? POINTS_PER_QUESTION : 0,
                analysis == null ? "" : analysis
        );
    }

    private boolean isObjective(Question question) {
        return switch (question.type()) {
            case SINGLE_CHOICE, MULTIPLE_CHOICE, TRUE_FALSE, FILL_BLANK -> true;
            case SHORT_ANSWER, PROGRAMMING -> false;
        };
    }

    private boolean answersMatch(Question question, String submittedAnswer) {
        if (submittedAnswer == null) {
            return false;
        }
        if (question.type() == com.studycollection.question.domain.QuestionType.MULTIPLE_CHOICE) {
            return normalizeMultipleChoiceAnswer(question.answer())
                    .equals(normalizeMultipleChoiceAnswer(submittedAnswer));
        }
        return question.answer().trim().equalsIgnoreCase(submittedAnswer.trim());
    }

    private String normalizeMultipleChoiceAnswer(String answer) {
        String compact = answer.toUpperCase(Locale.ROOT)
                .replaceAll("[\\s,，、;；|/]+", "");
        if (!compact.matches("[A-Z]+")) {
            return "";
        }
        return compact.chars()
                .distinct()
                .sorted()
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private void recordStats(Long userId, List<PracticeResultItem> items) {
        if (userId == null) {
            return;
        }
        int answered = items.size();
        int graded = (int) items.stream().filter(PracticeResultItem::autoGraded).count();
        int correct = (int) items.stream().filter(item -> Boolean.TRUE.equals(item.correct())).count();
        statsRepository.add(userId, answered, graded, correct);
    }

    private void recordAttempts(Long userId, List<PracticeResultItem> items) {
        if (userId == null) {
            return;
        }
        String referenceId = UUID.randomUUID().toString();
        Instant attemptedAt = clock.instant();
        attemptRepository.saveAll(items.stream().map(item -> {
            Question question = questionRepository.findById(item.questionId());
            return new LearningAttempt(
                    null,
                    userId,
                    LearningActivityType.PRACTICE,
                    referenceId,
                    question.id(),
                    question.title(),
                    question.type(),
                    question.difficulty(),
                    question.knowledgePoint(),
                    item.submittedAnswer(),
                    item.autoGraded(),
                    item.correct(),
                    item.score(),
                    attemptedAt
            );
        }).toList());
    }

}
