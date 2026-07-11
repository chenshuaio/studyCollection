package com.studycollection.exam.api;

import com.studycollection.common.api.ApiResponse;
import com.studycollection.common.security.AuthenticatedUser;
import com.studycollection.exam.app.PracticeStatsRepository;
import com.studycollection.question.app.QuestionRepository;
import com.studycollection.question.domain.Question;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/practice")
public class PracticeController {
    private static final int POINTS_PER_QUESTION = 10;
    private final QuestionRepository questionRepository;
    private final PracticeStatsRepository statsRepository;

    public PracticeController(QuestionRepository questionRepository, PracticeStatsRepository statsRepository) {
        this.questionRepository = questionRepository;
        this.statsRepository = statsRepository;
    }

    @PostMapping("/submit")
    public ApiResponse<PracticeResult> submit(
            @RequestAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE) AuthenticatedUser currentUser,
            @RequestBody PracticeSubmitRequest request
    ) {
        if (request == null || request.answers() == null || request.answers().isEmpty()) {
            throw new IllegalArgumentException("至少提交一道题目答案");
        }
        List<PracticeResultItem> items = request.answers().stream()
                .map(this::scoreAnswer)
                .toList();
        int score = items.stream().mapToInt(PracticeResultItem::score).sum();
        recordStats(currentUser.userId(), items);
        return ApiResponse.success(new PracticeResult(score, items.size() * POINTS_PER_QUESTION, items));
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
        boolean correct = answersMatch(question, answer.answer());
        return new PracticeResultItem(
                answer.questionId(),
                answer.answer(),
                correctAnswer,
                correct,
                correct ? POINTS_PER_QUESTION : 0,
                analysis == null ? "" : analysis
        );
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
        int correct = (int) items.stream().filter(PracticeResultItem::correct).count();
        statsRepository.add(userId, answered, correct);
    }

}
