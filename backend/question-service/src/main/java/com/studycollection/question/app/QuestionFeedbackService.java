package com.studycollection.question.app;

import com.studycollection.question.domain.FeedbackStatus;
import com.studycollection.question.domain.FeedbackType;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionFeedback;
import com.studycollection.question.domain.QuestionRevision;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class QuestionFeedbackService {
    private final QuestionFeedbackRepository feedbackRepository;
    private final QuestionRepository questionRepository;

    public QuestionFeedbackService(
            QuestionFeedbackRepository feedbackRepository,
            QuestionRepository questionRepository
    ) {
        this.feedbackRepository = feedbackRepository;
        this.questionRepository = questionRepository;
    }

    public QuestionFeedback submit(Long userId, Long questionId, FeedbackType type, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("反馈内容不能为空");
        }
        return feedbackRepository.saveFeedback(new QuestionFeedback(
                null,
                userId,
                questionId,
                type,
                content,
                FeedbackStatus.PENDING
        ));
    }

    public QuestionFeedback find(Long feedbackId) {
        return feedbackRepository.findFeedback(feedbackId);
    }

    public List<QuestionFeedback> pending() {
        return feedbackRepository.findByStatus(FeedbackStatus.PENDING);
    }

    public List<QuestionFeedback> byUser(Long userId) {
        return feedbackRepository.findByUserId(userId);
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
        QuestionFeedback feedback = find(feedbackId);
        requireReviewable(feedback);
        validateReview(adminUserId, reviewNote);
        if (changeSummary == null || changeSummary.isBlank()) {
            throw new IllegalArgumentException("修订说明不能为空");
        }
        applyQuestionRevision(feedback.questionId(), correctedAnswer, correctedAnalysis);
        updateStatus(feedback, FeedbackStatus.ACCEPTED);
        return feedbackRepository.saveRevision(new QuestionRevision(
                null,
                feedback.questionId(),
                feedback.id(),
                adminUserId,
                changeSummary,
                reviewNote
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

    private QuestionFeedback review(Long feedbackId, Long adminUserId, String reviewNote, FeedbackStatus status) {
        QuestionFeedback feedback = find(feedbackId);
        requireReviewable(feedback);
        validateReview(adminUserId, reviewNote);
        return updateStatus(feedback, status);
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

    private void applyQuestionRevision(Long questionId, String correctedAnswer, String correctedAnalysis) {
        if (questionRepository == null || (isBlank(correctedAnswer) && isBlank(correctedAnalysis))) {
            return;
        }
        Question current = questionRepository.findById(questionId);
        Question revised = new Question(
                current.id(),
                current.title(),
                current.type(),
                current.difficulty(),
                current.knowledgePoint(),
                isBlank(correctedAnswer) ? current.answer() : correctedAnswer,
                isBlank(correctedAnalysis) ? current.analysis() : correctedAnalysis
        );
        questionRepository.update(revised);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private QuestionFeedback updateStatus(QuestionFeedback feedback, FeedbackStatus status) {
        QuestionFeedback reviewed = new QuestionFeedback(
                feedback.id(),
                feedback.userId(),
                feedback.questionId(),
                feedback.type(),
                feedback.content(),
                status
        );
        return feedbackRepository.saveFeedback(reviewed);
    }
}
