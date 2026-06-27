package com.studycollection.question.api;

import com.studycollection.common.api.ApiResponse;
import com.studycollection.question.app.QuestionRepository;
import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/questions")
public class QuestionController {
    private final QuestionRepository questionRepository;

    public QuestionController(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @PostMapping
    public ApiResponse<Question> create(@RequestBody CreateQuestionRequest request) {
        return ApiResponse.success(questionRepository.save(new Question(
                null,
                request.title(),
                request.type(),
                request.difficulty(),
                request.knowledgePoint(),
                request.answer(),
                request.analysis()
        )));
    }

    @GetMapping
    public ApiResponse<List<Question>> search(
            @RequestParam("knowledgePoint") String knowledgePoint,
            @RequestParam("difficulty") Difficulty difficulty,
            @RequestParam("type") QuestionType type
    ) {
        return ApiResponse.success(questionRepository.search(knowledgePoint, difficulty, type));
    }
}
