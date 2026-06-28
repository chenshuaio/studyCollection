package com.studycollection.question.api;

import com.studycollection.common.api.ApiResponse;
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
import org.springframework.web.bind.annotation.RestController;

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
    public ApiResponse<PendingQuestion> submit(@RequestBody SubmitPendingQuestionRequest request) {
        return ApiResponse.success(pendingQuestionRepository.save(new PendingQuestion(
                null,
                request.submitterUserId(),
                request.title(),
                request.type(),
                request.difficulty(),
                request.knowledgePoint(),
                request.answer(),
                request.analysis(),
                PendingQuestionStatus.PENDING
        )));
    }

    @GetMapping
    public ApiResponse<List<PendingQuestion>> pending() {
        return ApiResponse.success(pendingQuestionRepository.findByStatus(PendingQuestionStatus.PENDING));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<Question> approve(@PathVariable("id") Long id) {
        PendingQuestion pendingQuestion = pendingQuestionRepository.find(id);
        Question saved = questionRepository.save(pendingQuestion.toQuestion());
        pendingQuestionRepository.save(pendingQuestion.withStatus(PendingQuestionStatus.APPROVED));
        return ApiResponse.success(saved);
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<PendingQuestion> reject(@PathVariable("id") Long id) {
        PendingQuestion pendingQuestion = pendingQuestionRepository.find(id);
        return ApiResponse.success(pendingQuestionRepository.save(pendingQuestion.withStatus(PendingQuestionStatus.REJECTED)));
    }
}
