package com.studycollection.question.app;

import com.studycollection.question.domain.FeedbackStatus;
import com.studycollection.question.domain.FeedbackType;
import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionFeedback;
import com.studycollection.question.domain.QuestionRevision;
import com.studycollection.question.domain.QuestionSnapshot;
import com.studycollection.question.domain.QuestionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class QuestionFeedbackService {
    private final QuestionFeedbackRepository feedbackRepository;
    private final QuestionRepository questionRepository;
    private final Clock clock;

    @Autowired
    public QuestionFeedbackService(
            QuestionFeedbackRepository feedbackRepository,
            QuestionRepository questionRepository
    ) {
        this(feedbackRepository, questionRepository, Clock.systemUTC());
    }

    public QuestionFeedbackService(
            QuestionFeedbackRepository feedbackRepository,
            QuestionRepository questionRepository,
            Clock clock
    ) {
        this.feedbackRepository = feedbackRepository;
        this.questionRepository = questionRepository;
        this.clock = clock;
    }

    public QuestionFeedback submit(Long userId, Long questionId, FeedbackType type, String content) {
        return submit(userId, questionId, type, content, "", "UNKNOWN", "");
    }

    public QuestionFeedback submit(
            Long userId,
            Long questionId,
            FeedbackType type,
            String content,
            String submittedAnswer,
            String sourceContext,
            String sourceReference
    ) {
        if (questionId == null) {
            throw new IllegalArgumentException("题目 ID 不能为空");
        }
        if (type == null) {
            throw new IllegalArgumentException("反馈类型不能为空");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("反馈内容不能为空");
        }
        return feedbackRepository.saveFeedback(new QuestionFeedback(
                null,
                userId,
                questionId,
                type,
                content.trim(),
                submittedAnswer,
                normalizeSource(sourceContext),
                sourceReference,
                FeedbackStatus.PENDING,
                clock.instant(),
                null,
                "",
                null
        ));
    }

    public QuestionFeedback find(Long feedbackId) {
        return feedbackRepository.findFeedback(feedbackId);
    }

    public List<QuestionFeedback> pending() {
        return feedbackRepository.findByStatus(FeedbackStatus.PENDING);
    }

    public List<QuestionFeedbackGroup> pendingGroups() {
        List<QuestionFeedback> reviewable = feedbackRepository.findByStatuses(Set.of(
                FeedbackStatus.PENDING,
                FeedbackStatus.NEEDS_REVIEW
        ));
        Map<GroupKey, List<QuestionFeedback>> grouped = new LinkedHashMap<>();
        reviewable.forEach(feedback -> grouped
                .computeIfAbsent(new GroupKey(feedback.questionId(), feedback.type()), ignored -> new ArrayList<>())
                .add(feedback));
        return grouped.entrySet().stream()
                .map(entry -> toGroup(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(QuestionFeedbackGroup::latestAt).reversed()
                        .thenComparing(QuestionFeedbackGroup::questionId))
                .toList();
    }

    public List<QuestionFeedback> byUser(Long userId) {
        return feedbackRepository.findByUserId(userId);
    }

    public List<QuestionRevision> revisions(Long questionId) {
        return feedbackRepository.findRevisionsByQuestionId(questionId);
    }

    @Transactional
    public QuestionRevision accept(
            Long feedbackId,
            Long adminUserId,
            String changeSummary,
            String reviewNote,
            String correctedAnswer,
            String correctedAnalysis
    ) {
        return accept(
                feedbackId,
                adminUserId,
                changeSummary,
                reviewNote,
                null,
                null,
                null,
                null,
                correctedAnswer,
                correctedAnalysis
        );
    }

    @Transactional
    public QuestionRevision accept(
            Long feedbackId,
            Long adminUserId,
            String changeSummary,
            String reviewNote,
            String correctedTitle,
            QuestionType correctedType,
            Difficulty correctedDifficulty,
            String correctedKnowledgePoint,
            String correctedAnswer,
            String correctedAnalysis
    ) {
        return acceptGroup(
                List.of(feedbackId),
                adminUserId,
                changeSummary,
                reviewNote,
                correctedTitle,
                correctedType,
                correctedDifficulty,
                correctedKnowledgePoint,
                correctedAnswer,
                correctedAnalysis
        );
    }

    @Transactional
    public QuestionRevision acceptGroup(
            List<Long> feedbackIds,
            Long adminUserId,
            String changeSummary,
            String reviewNote,
            String correctedAnswer,
            String correctedAnalysis
    ) {
        return acceptGroup(
                feedbackIds,
                adminUserId,
                changeSummary,
                reviewNote,
                null,
                null,
                null,
                null,
                correctedAnswer,
                correctedAnalysis
        );
    }

    @Transactional
    public QuestionRevision acceptGroup(
            List<Long> feedbackIds,
            Long adminUserId,
            String changeSummary,
            String reviewNote,
            String correctedTitle,
            QuestionType correctedType,
            Difficulty correctedDifficulty,
            String correctedKnowledgePoint,
            String correctedAnswer,
            String correctedAnalysis
    ) {
        validateReview(adminUserId, reviewNote);
        if (changeSummary == null || changeSummary.isBlank()) {
            throw new IllegalArgumentException("修订说明不能为空");
        }
        List<Long> distinctIds = feedbackIds == null
                ? List.of()
                : new ArrayList<>(new LinkedHashSet<>(feedbackIds));
        if (distinctIds.isEmpty()) {
            throw new IllegalArgumentException("至少选择一条反馈");
        }
        List<QuestionFeedback> feedbacks = distinctIds.stream().map(this::find).toList();
        feedbacks.forEach(this::requireReviewable);
        QuestionFeedback primary = feedbacks.get(0);
        boolean sameGroup = feedbacks.stream().allMatch(feedback ->
                feedback.questionId().equals(primary.questionId()) && feedback.type() == primary.type());
        if (!sameGroup) {
            throw new IllegalArgumentException("只能合并同一题目和同一类型的反馈");
        }

        Question before = questionRepository.findById(primary.questionId());
        String source = questionRepository.findSourceById(primary.questionId());
        Question after = revisedQuestion(
                before,
                correctedTitle,
                correctedType,
                correctedDifficulty,
                correctedKnowledgePoint,
                correctedAnswer,
                correctedAnalysis
        );
        questionRepository.update(after);
        feedbacks.forEach(feedback -> updateStatus(
                feedback,
                FeedbackStatus.ACCEPTED,
                adminUserId,
                reviewNote
        ));
        return feedbackRepository.saveRevision(new QuestionRevision(
                null,
                primary.questionId(),
                primary.id(),
                distinctIds,
                adminUserId,
                changeSummary.trim(),
                reviewNote.trim(),
                QuestionSnapshot.from(before, source),
                QuestionSnapshot.from(after, source),
                primary.type() == FeedbackType.ANSWER_ERROR
                        && !Objects.equals(before.answer(), after.answer()),
                clock.instant()
        ));
    }

    @Transactional
    public QuestionFeedback reject(Long feedbackId, Long adminUserId, String reviewNote) {
        return review(feedbackId, adminUserId, reviewNote, FeedbackStatus.REJECTED);
    }

    @Transactional
    public QuestionFeedback markNeedsReview(Long feedbackId, Long adminUserId, String reviewNote) {
        return review(feedbackId, adminUserId, reviewNote, FeedbackStatus.NEEDS_REVIEW);
    }

    private QuestionFeedback review(
            Long feedbackId,
            Long adminUserId,
            String reviewNote,
            FeedbackStatus status
    ) {
        QuestionFeedback feedback = find(feedbackId);
        requireReviewable(feedback);
        validateReview(adminUserId, reviewNote);
        return updateStatus(feedback, status, adminUserId, reviewNote);
    }

    private QuestionFeedbackGroup toGroup(GroupKey key, List<QuestionFeedback> feedbacks) {
        Question question = questionRepository.findById(key.questionId());
        List<QuestionFeedback> sorted = feedbacks.stream()
                .sorted(Comparator.comparing(QuestionFeedback::createdAt)
                        .thenComparing(QuestionFeedback::id)
                        .reversed())
                .toList();
        return new QuestionFeedbackGroup(
                question.id(),
                question.title(),
                question.type(),
                question.difficulty(),
                question.knowledgePoint(),
                question.answer(),
                question.analysis(),
                questionRepository.findSourceById(question.id()),
                key.type(),
                sorted.size(),
                sorted.get(0).createdAt(),
                sorted
        );
    }

    private Question revisedQuestion(
            Question current,
            String correctedTitle,
            QuestionType correctedType,
            Difficulty correctedDifficulty,
            String correctedKnowledgePoint,
            String correctedAnswer,
            String correctedAnalysis
    ) {
        return new Question(
                current.id(),
                isBlank(correctedTitle) ? current.title() : correctedTitle.trim(),
                correctedType == null ? current.type() : correctedType,
                correctedDifficulty == null ? current.difficulty() : correctedDifficulty,
                isBlank(correctedKnowledgePoint) ? current.knowledgePoint() : correctedKnowledgePoint.trim(),
                isBlank(correctedAnswer) ? current.answer() : correctedAnswer.trim(),
                isBlank(correctedAnalysis) ? current.analysis() : correctedAnalysis.trim()
        );
    }

    private void requireReviewable(QuestionFeedback feedback) {
        if (feedback.status() != FeedbackStatus.PENDING && feedback.status() != FeedbackStatus.NEEDS_REVIEW) {
            throw new IllegalArgumentException("反馈已处理");
        }
    }

    private void validateReview(Long adminUserId, String reviewNote) {
        if (adminUserId == null) {
            throw new IllegalArgumentException("管理员用户不能为空");
        }
        if (reviewNote == null || reviewNote.isBlank()) {
            throw new IllegalArgumentException("审核备注不能为空");
        }
    }

    private QuestionFeedback updateStatus(
            QuestionFeedback feedback,
            FeedbackStatus status,
            Long adminUserId,
            String reviewNote
    ) {
        return feedbackRepository.saveFeedback(feedback.reviewed(
                status,
                adminUserId,
                reviewNote.trim(),
                clock.instant()
        ));
    }

    private String normalizeSource(String value) {
        if (value == null || value.isBlank()) {
            return "UNKNOWN";
        }
        String source = value.trim().toUpperCase();
        if (!Set.of("PRACTICE", "EXAM", "MISTAKE_BOOK", "QUESTION_DETAIL", "UNKNOWN").contains(source)) {
            throw new IllegalArgumentException("反馈来源不受支持");
        }
        return source;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record GroupKey(Long questionId, FeedbackType type) {
    }
}
