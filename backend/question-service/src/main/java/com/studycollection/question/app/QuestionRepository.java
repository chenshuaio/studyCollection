package com.studycollection.question.app;

import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.Question;
import com.studycollection.question.domain.QuestionBankScope;
import com.studycollection.question.domain.QuestionType;

import java.util.List;

public interface QuestionRepository {
    Question save(Question question);

    Question findById(Long id);

    default String findSourceById(Long id) {
        findById(id);
        return "LOCAL_UPLOAD";
    }

    Question update(Question question);

    List<Question> search(String keyword, String knowledgePoint, Difficulty difficulty, QuestionType type);

    default List<Question> searchAccessible(
            Long userId,
            QuestionBankScope scope,
            String keyword,
            String knowledgePoint,
            Difficulty difficulty,
            QuestionType type
    ) {
        QuestionBankScope effectiveScope = scope == null ? QuestionBankScope.ALL : scope;
        return search(keyword, knowledgePoint, difficulty, type).stream()
                .filter(question -> question.isPublic() || question.isOwnedBy(userId))
                .filter(question -> effectiveScope != QuestionBankScope.PUBLIC || question.isPublic())
                .filter(question -> effectiveScope != QuestionBankScope.PERSONAL || question.isOwnedBy(userId))
                .toList();
    }

    default Question findAccessibleById(Long id, Long userId) {
        Question question = findById(id);
        if (!question.isPublic() && !question.isOwnedBy(userId)) {
            throw new IllegalArgumentException("题目不存在或无权访问");
        }
        return question;
    }

    default void deleteOwnedById(Long id, Long userId) {
        Question question = findById(id);
        if (!question.isOwnedBy(userId)) {
            throw new IllegalArgumentException("题目不存在或无权访问");
        }
        deleteById(id);
    }

    void deleteById(Long id);
}
