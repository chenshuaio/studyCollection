package com.studycollection.question.api;

import com.studycollection.common.api.ApiResponse;
import com.studycollection.question.app.QuestionRepository;
import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "knowledgePoint", required = false) String knowledgePoint,
            @RequestParam(value = "difficulty", required = false) Difficulty difficulty,
            @RequestParam(value = "type", required = false) QuestionType type
    ) {
        return ApiResponse.success(questionRepository.search(keyword, knowledgePoint, difficulty, type));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Long> deleteQuestion(@PathVariable("id") Long id) {
        questionRepository.deleteById(id);
        return ApiResponse.success(id);
    }
}
