package com.studycollection.local;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = LocalStudyCollectionApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class LocalStudyCollectionApplicationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void exposesLocalLearningApisInOneProcess() {
        assertOk(post("/auth/login", Map.of("username", "user", "password", "user123")));
        assertOk(post("/questions", Map.of(
                "title", "HashMap 默认负载因子是多少？",
                "type", "SINGLE_CHOICE",
                "difficulty", "INTERMEDIATE",
                "knowledgePoint", "集合框架",
                "answer", "A",
                "analysis", "HashMap 默认负载因子是 0.75。"
        )));
        assertOk(post("/questions", Map.of(
                "title", "Java 中 int 默认值是多少？",
                "type", "SINGLE_CHOICE",
                "difficulty", "BEGINNER",
                "knowledgePoint", "Java 基础",
                "answer", "A",
                "analysis", "int 成员变量默认值为 0。"
        )));
        assertOk(get("/questions"));
        assertOk(get("/questions?keyword=HashMap"));
        assertOk(get("/questions?knowledgePoint=集合框架&difficulty=INTERMEDIATE&type=SINGLE_CHOICE"));
        assertOk(post("/imports/preview", Map.of("content", """
                ## 单选题
                题目: Java 中 int 默认值是多少？
                答案: A
                知识点: Java 基础
                难度: BEGINNER
                """)));
        assertOk(post("/practice/submit", Map.of("answers", List.of(Map.of("questionId", 1, "answer", "A")))));
        assertOk(post("/exams/custom", Map.of(
                "name", "集合专项测试",
                "durationMinutes", 45,
                "questionIds", List.of(1, 2)
        )));
        assertOk(post("/reports/learning", Map.of(
                "mode", "OFFLINE_RULES",
                "results", List.of(
                        Map.of("knowledgePoint", "集合框架", "correct", true),
                        Map.of("knowledgePoint", "JVM", "correct", false)
                )
        )));
        assertOk(post("/mistakes", Map.of(
                "userId", 7,
                "questionId", 1,
                "questionTitle", "HashMap 默认负载因子是多少？",
                "knowledgePoint", "集合框架",
                "status", "PENDING"
        )));
        assertOk(get("/mistakes?userId=7"));
        assertOk(post("/questions/feedback", Map.of(
                "userId", 7,
                "questionId", 1,
                "type", "ANSWER_ERROR",
                "content", "标准答案应为 B"
        )));
        assertOk(get("/questions/feedback/pending"));
        assertOk(post("/questions/feedback/1/accept", Map.of(
                "adminUserId", 1,
                "changeSummary", "根据反馈修订答案",
                "reviewNote", "用户反馈属实"
        )));
        assertOk(post("/questions/feedback", Map.of(
                "userId", 7,
                "questionId", 1,
                "type", "ANSWER_ERROR",
                "content", "原答案正确，请驳回"
        )));
        assertOk(post("/questions/feedback/2/reject", Map.of(
                "adminUserId", 1,
                "reviewNote", "核对后确认原答案正确"
        )));
        assertOk(post("/questions/feedback", Map.of(
                "userId", 8,
                "questionId", 2,
                "type", "EXPLANATION_ERROR",
                "content", "解析需要教研复核"
        )));
        assertOk(post("/questions/feedback/3/needs-review", Map.of(
                "adminUserId", 1,
                "reviewNote", "交给教研二次确认"
        )));
    }

    private ResponseEntity<String> post(String path, Object body) {
        return restTemplate.postForEntity(url(path), body, String.class);
    }

    private ResponseEntity<String> get(String path) {
        return restTemplate.getForEntity(url(path), String.class);
    }

    private String url(String path) {
        return "http://127.0.0.1:" + port + path;
    }

    private static void assertOk(ResponseEntity<String> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"code\":\"OK\"");
    }
}
