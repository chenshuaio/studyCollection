package com.studycollection.question.api;

import com.studycollection.common.api.ApiResponse;
import com.studycollection.common.security.AdminOnly;
import com.studycollection.common.security.AuthenticatedUser;
import com.studycollection.question.app.PendingQuestionRepository;
import com.studycollection.question.app.QuestionRepository;
import com.studycollection.question.domain.PendingQuestion;
import com.studycollection.question.domain.PendingQuestionStatus;
import com.studycollection.question.domain.Question;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RestController
@RequestMapping("/questions/pending")
public class PendingQuestionController {
    private final PendingQuestionRepository pendingQuestionRepository;
    private final QuestionRepository questionRepository;

    public PendingQuestionController(PendingQuestionRepository pendingQuestionRepository, QuestionRepository questionRepository) {
        this.pendingQuestionRepository = pendingQuestionRepository;
        this.questionRepository = questionRepository;
    }

    @PostMapping
    public ApiResponse<PendingQuestion> submit(
            @RequestAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE) AuthenticatedUser currentUser,
            @RequestBody SubmitPendingQuestionRequest request
    ) {
        return ApiResponse.success(pendingQuestionRepository.save(new PendingQuestion(
                null,
                currentUser.userId(),
                request.title(),
                request.type(),
                request.difficulty(),
                request.knowledgePoint(),
                request.answer(),
                request.analysis(),
                request.targetScope(),
                PendingQuestionStatus.PENDING
        )));
    }

    @GetMapping
    @AdminOnly
    public ApiResponse<List<PendingQuestion>> pending() {
        return ApiResponse.success(pendingQuestionRepository.findByStatus(PendingQuestionStatus.PENDING));
    }

    @PostMapping("/{id}/approve")
    @AdminOnly
    @Transactional
    public ApiResponse<Question> approve(@PathVariable("id") Long id) {
        PendingQuestion pendingQuestion = pendingQuestionRepository.find(id);
        requirePending(pendingQuestion);
        Question saved = questionRepository.save(pendingQuestion.toQuestion());
        pendingQuestionRepository.save(pendingQuestion.withStatus(PendingQuestionStatus.APPROVED));
        return ApiResponse.success(saved);
    }

    @PostMapping("/{id}/reject")
    @AdminOnly
    public ApiResponse<PendingQuestion> reject(@PathVariable("id") Long id) {
        PendingQuestion pendingQuestion = pendingQuestionRepository.find(id);
        requirePending(pendingQuestion);
        return ApiResponse.success(pendingQuestionRepository.save(pendingQuestion.withStatus(PendingQuestionStatus.REJECTED)));
    }

    private void requirePending(PendingQuestion pendingQuestion) {
        if (pendingQuestion.status() != PendingQuestionStatus.PENDING) {
            throw new IllegalArgumentException("待审核题目已处理");
        }
    }
}
