package com.studycollection.exam.app;

import com.studycollection.question.app.QuestionRepository;
import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionBankScope;
import com.studycollection.question.domain.QuestionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
public class PracticeGenerator {
    private final QuestionRepository questionRepository;
    private final Random random;

    @Autowired
    public PracticeGenerator(QuestionRepository questionRepository) {
        this(questionRepository, new SecureRandom());
    }

    public PracticeGenerator(QuestionRepository questionRepository, Random random) {
        this.questionRepository = questionRepository;
        this.random = random;
    }

    public List<Question> generate(
            Long userId,
            QuestionBankScope scope,
            String knowledgePoint,
            Difficulty difficulty,
            QuestionType type,
            int count
    ) {
        if (count < 1 || count > 100) {
            throw new IllegalArgumentException("练习题目数量必须在 1 到 100 之间");
        }
        String normalizedKnowledgePoint = knowledgePoint == null || knowledgePoint.isBlank()
                ? null
                : knowledgePoint.trim();
        List<Question> available = new ArrayList<>(questionRepository.searchAccessible(
                userId,
                scope,
                null,
                normalizedKnowledgePoint,
                difficulty,
                type
        ));
        if (available.isEmpty()) {
            throw new IllegalArgumentException("没有符合条件的可用题目");
        }
        Collections.shuffle(available, random);
        return List.copyOf(available.subList(0, Math.min(count, available.size())));
    }
}
