package com.studycollection.exam.domain;

import com.studycollection.question.domain.QuestionType;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record ExamSession(
        Long id,
        Long userId,
        String name,
        int durationMinutes,
        ExamStatus status,
        Instant startedAt,
        Instant expiresAt,
        Instant submittedAt,
        Integer score,
        Integer totalScore,
        List<ExamQuestionSnapshot> questions,
        Map<Long, ExamAnswer> answers
) {
    private static final int POINTS_PER_OBJECTIVE_QUESTION = 10;

    public ExamSession {
        questions = List.copyOf(questions);
        answers = Collections.unmodifiableMap(new LinkedHashMap<>(answers));
    }

    public static ExamSession start(
            Long userId,
            String name,
            int durationMinutes,
            Instant startedAt,
            List<ExamQuestionSnapshot> questions
    ) {
        if (userId == null) {
            throw new IllegalArgumentException("用户编号不能为空");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("试卷名称不能为空");
        }
        if (durationMinutes < 1 || durationMinutes > 480) {
            throw new IllegalArgumentException("考试时长必须在 1 到 480 分钟之间");
        }
        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("至少选择一道题目");
        }
        return new ExamSession(
                null,
                userId,
                name.trim(),
                durationMinutes,
                ExamStatus.IN_PROGRESS,
                startedAt,
                startedAt.plus(Duration.ofMinutes(durationMinutes)),
                null,
                null,
                null,
                questions,
                Map.of()
        );
    }

    public ExamSession withId(Long persistedId) {
        return new ExamSession(
                persistedId,
                userId,
                name,
                durationMinutes,
                status,
                startedAt,
                expiresAt,
                submittedAt,
                score,
                totalScore,
                questions,
                answers
        );
    }

    public ExamSession saveAnswer(Long questionId, String submittedAnswer) {
        requireInProgress();
        question(questionId);
        Map<Long, ExamAnswer> updatedAnswers = new LinkedHashMap<>(answers);
        updatedAnswers.put(questionId, ExamAnswer.saved(questionId, submittedAnswer));
        return copy(status, submittedAt, score, totalScore, updatedAnswers);
    }

    public ExamSession complete(Instant completedAt) {
        requireInProgress();
        Map<Long, ExamAnswer> gradedAnswers = new LinkedHashMap<>();
        int earnedScore = 0;
        int availableScore = 0;

        for (ExamQuestionSnapshot question : questions) {
            String submittedAnswer = answers.getOrDefault(
                    question.questionId(),
                    ExamAnswer.saved(question.questionId(), "")
            ).submittedAnswer();
            ExamAnswer answer;
            if (question.isObjective()) {
                boolean correct = answersMatch(question.type(), question.correctAnswer(), submittedAnswer);
                answer = ExamAnswer.graded(
                        question.questionId(),
                        submittedAnswer,
                        correct,
                        POINTS_PER_OBJECTIVE_QUESTION
                );
                earnedScore += answer.score();
                availableScore += POINTS_PER_OBJECTIVE_QUESTION;
            } else {
                answer = ExamAnswer.saved(question.questionId(), submittedAnswer);
            }
            gradedAnswers.put(question.questionId(), answer);
        }

        return copy(ExamStatus.SUBMITTED, completedAt, earnedScore, availableScore, gradedAnswers);
    }

    public long remainingSeconds(Instant now) {
        if (status == ExamStatus.SUBMITTED || !now.isBefore(expiresAt)) {
            return 0;
        }
        return Math.max(0, Duration.between(now, expiresAt).getSeconds());
    }

    public ExamQuestionSnapshot question(Long questionId) {
        return questions.stream()
                .filter(candidate -> candidate.questionId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("考试中不存在该题目"));
    }

    private ExamSession copy(
            ExamStatus nextStatus,
            Instant nextSubmittedAt,
            Integer nextScore,
            Integer nextTotalScore,
            Map<Long, ExamAnswer> nextAnswers
    ) {
        return new ExamSession(
                id,
                userId,
                name,
                durationMinutes,
                nextStatus,
                startedAt,
                expiresAt,
                nextSubmittedAt,
                nextScore,
                nextTotalScore,
                questions,
                nextAnswers
        );
    }

    private void requireInProgress() {
        if (status != ExamStatus.IN_PROGRESS) {
            throw new IllegalStateException("考试已经提交，不能再次修改");
        }
    }

    private static boolean answersMatch(QuestionType type, String correctAnswer, String submittedAnswer) {
        if (correctAnswer == null || submittedAnswer == null) {
            return false;
        }
        if (type == QuestionType.MULTIPLE_CHOICE) {
            return normalizeMultipleChoice(correctAnswer).equals(normalizeMultipleChoice(submittedAnswer));
        }
        return correctAnswer.trim().equalsIgnoreCase(submittedAnswer.trim());
    }

    private static String normalizeMultipleChoice(String answer) {
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
}
