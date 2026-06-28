package com.studycollection.exam.api;

import com.studycollection.common.api.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/practice")
public class PracticeController {
    private static final int POINTS_PER_QUESTION = 10;
    private final Map<Long, PracticeQuestion> questionBank = sampleQuestions();
    private final Map<Long, PracticeStats> statsByUser = new ConcurrentHashMap<>();

    @PostMapping("/submit")
    public ApiResponse<PracticeResult> submit(@RequestBody PracticeSubmitRequest request) {
        List<PracticeResultItem> items = request.answers().stream()
                .map(this::scoreAnswer)
                .toList();
        int score = items.stream().mapToInt(PracticeResultItem::score).sum();
        recordStats(request.userId(), items);
        return ApiResponse.success(new PracticeResult(score, items.size() * POINTS_PER_QUESTION, items));
    }

    @GetMapping("/stats")
    public ApiResponse<PracticeStats> stats(@RequestParam("userId") Long userId) {
        return ApiResponse.success(statsByUser.getOrDefault(userId, new PracticeStats(userId, 0, 0)));
    }

    private PracticeResultItem scoreAnswer(PracticeAnswer answer) {
        PracticeQuestion question = questionBank.get(answer.questionId());
        boolean correct = question.correctAnswer().equalsIgnoreCase(answer.answer());
        return new PracticeResultItem(
                answer.questionId(),
                answer.answer(),
                question.correctAnswer(),
                correct,
                correct ? POINTS_PER_QUESTION : 0,
                question.analysis()
        );
    }

    private void recordStats(Long userId, List<PracticeResultItem> items) {
        if (userId == null) {
            return;
        }
        int answered = items.size();
        int correct = (int) items.stream().filter(PracticeResultItem::correct).count();
        statsByUser.merge(
                userId,
                new PracticeStats(userId, answered, correct),
                (existing, current) -> new PracticeStats(
                        userId,
                        existing.answeredQuestionCount() + current.answeredQuestionCount(),
                        existing.correctQuestionCount() + current.correctQuestionCount()
                )
        );
    }

    private static Map<Long, PracticeQuestion> sampleQuestions() {
        Map<Long, PracticeQuestion> questions = new LinkedHashMap<>();
        questions.put(1L, new PracticeQuestion("A", "HashMap 默认负载因子是 0.75，达到阈值后会触发扩容。"));
        questions.put(2L, new PracticeQuestion("true", "Java 基本类型局部变量没有默认值，必须先赋值再使用。"));
        questions.put(3L, new PracticeQuestion("A", "ArrayList 在容量不足以容纳新增元素时会触发扩容。"));
        return questions;
    }

    private record PracticeQuestion(String correctAnswer, String analysis) {
    }
}
