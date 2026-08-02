package com.studycollection.local;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
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

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginReturnsCompleteAuthenticatedUserIdentity() throws Exception {
        Session session = login("user", "user123");

        assertThat(session.userId()).isPositive();
        assertThat(session.username()).isEqualTo("user");
        assertThat(session.role()).isEqualTo("USER");
        assertThat(session.token()).isNotBlank();
    }

    @Test
    void protectedEndpointsRequireAuthentication() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/questions"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).contains("\"code\":\"UNAUTHORIZED\"");
    }

    @Test
    void ordinaryUsersCannotCallAdministratorEndpoints() throws Exception {
        Session user = login("user", "user123");

        ResponseEntity<String> response = post("/questions", Map.of(
                "title", "越权创建的题目",
                "type", "SINGLE_CHOICE",
                "difficulty", "BEGINNER",
                "knowledgePoint", "权限测试",
                "answer", "A",
                "analysis", "不应创建成功"
        ), user.token());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("\"code\":\"FORBIDDEN\"");
    }

    @Test
    void authenticatedIdentityCannotBeSpoofedAndScoringIgnoresClientCorrectAnswer() throws Exception {
        Session admin = login("admin", "admin123");
        Session user = login("user", "user123");
        long questionId = data(post("/questions", Map.of(
                "title", "安全审计唯一题目\nA. 错误选项\nB. 正确选项",
                "type", "SINGLE_CHOICE",
                "difficulty", "INTERMEDIATE",
                "knowledgePoint", "安全审计",
                "answer", "B",
                "analysis", "正确答案由服务端题库决定。"
        ), admin.token())).path("id").asLong();

        ResponseEntity<String> visibleQuestion = get("/questions?keyword=安全审计唯一题目", user.token());
        assertOk(visibleQuestion);
        assertThat(visibleQuestion.getBody()).contains("\"answer\":\"\"");
        assertThat(visibleQuestion.getBody()).doesNotContain("\"answer\":\"B\"");

        ResponseEntity<String> scored = post("/practice/submit", Map.of(
                "userId", admin.userId(),
                "answers", List.of(Map.of(
                        "questionId", questionId,
                        "answer", "A",
                        "correctAnswer", "A",
                        "analysis", "伪造解析"
                ))
        ), user.token());
        assertOk(scored);
        assertThat(scored.getBody()).contains("\"correct\":false");
        assertThat(scored.getBody()).contains("\"correctAnswer\":\"B\"");

        ResponseEntity<String> mistake = post("/mistakes", Map.of(
                "userId", admin.userId(),
                "questionId", questionId,
                "questionTitle", "安全审计唯一题目",
                "knowledgePoint", "安全审计",
                "status", "PENDING"
        ), user.token());
        assertOk(mistake);
        assertThat(data(mistake).path("userId").asLong()).isEqualTo(user.userId());

        ResponseEntity<String> listed = get("/mistakes?userId=" + admin.userId(), user.token());
        assertOk(listed);
        assertThat(listed.getBody()).contains("\"userId\":" + user.userId());
        assertThat(listed.getBody()).doesNotContain("\"userId\":" + admin.userId());
    }

    @Test
    void registeredUsersReceiveUsableOrdinaryUserSession() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        ResponseEntity<String> response = post("/auth/register", Map.of(
                "username", "learner-" + suffix,
                "password", "pass123456",
                "displayName", "学习者-" + suffix
        ));

        assertOk(response);
        JsonNode registered = data(response);
        assertThat(registered.path("role").asText()).isEqualTo("USER");
        assertThat(registered.path("userId").asLong()).isPositive();
        assertThat(registered.path("token").asText()).isNotBlank();
        assertOk(get("/questions", registered.path("token").asText()));
    }

    @Test
    void businessValidationErrorsUseUnifiedBadRequestResponse() throws Exception {
        Session user = login("user", "user123");

        ResponseEntity<String> response = post("/practice/submit", Map.of(
                "answers", List.of(Map.of("questionId", Long.MAX_VALUE, "answer", "A"))
        ), user.token());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("\"code\":\"VALIDATION_FAILED\"");
        assertThat(response.getBody()).contains("题目不存在");
    }

    @Test
    void brokenPdfUploadReturnsUnifiedBadRequest() throws Exception {
        Session user = login("user", "user123");
        ByteArrayResource brokenPdf = new ByteArrayResource("not a pdf".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "broken.pdf";
            }
        };
        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.APPLICATION_PDF);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new HttpEntity<>(brokenPdf, fileHeaders));
        HttpHeaders requestHeaders = headers(user.token());
        requestHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<String> response = restTemplate.exchange(
                url("/imports/knowledge/upload"),
                HttpMethod.POST,
                new HttpEntity<>(body, requestHeaders),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        assertThat(responseBody.path("code").asText()).isEqualTo("VALIDATION_FAILED");
        assertThat(responseBody.path("message").asText())
                .isEqualTo("PDF 无法解析，请确认文件未损坏且未加密。");
    }

    @Test
    void learningReportsUseAuthenticatedServerAttemptsAndPersistHistory() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        Session admin = login("admin", "admin123");
        JsonNode registered = data(post("/auth/register", Map.of(
                "username", "report-user-" + suffix,
                "password", "pass123456",
                "displayName", "报告用户-" + suffix
        )));
        String token = registered.path("token").asText();
        String knowledgePoint = "报告验证-" + suffix;
        long questionId = data(post("/questions", Map.of(
                "title", "可信报告验证题\nA. 错误答案\nB. 正确答案",
                "type", "SINGLE_CHOICE",
                "difficulty", "INTERMEDIATE",
                "knowledgePoint", knowledgePoint,
                "answer", "B",
                "analysis", "报告应使用服务端评分结果。"
        ), admin.token())).path("id").asLong();

        assertOk(post("/practice/submit", Map.of(
                "answers", List.of(Map.of("questionId", questionId, "answer", "A"))
        ), token));
        ResponseEntity<String> generated = post("/reports/learning", Map.of(
                "mode", "OFFLINE_RULES",
                "results", List.of(Map.of("knowledgePoint", "伪造知识点", "correct", true))
        ), token);

        assertOk(generated);
        JsonNode report = data(generated);
        assertThat(report.path("answeredQuestionCount").asInt()).isEqualTo(1);
        assertThat(report.path("gradedQuestionCount").asInt()).isEqualTo(1);
        assertThat(report.path("correctQuestionCount").asInt()).isZero();
        assertThat(report.path("weakestKnowledgePoint").asText()).isEqualTo(knowledgePoint);
        assertThat(report.path("strengtheningQuestions").get(0).has("answer")).isFalse();

        ResponseEntity<String> history = get("/reports/learning", token);
        assertOk(history);
        assertThat(data(history).size()).isEqualTo(1);
        assertThat(data(history).get(0).path("id").asLong()).isEqualTo(report.path("id").asLong());
    }

    @Test
    void invalidLoginReturnsAccountOrPasswordError() {
        ResponseEntity<String> response = post("/auth/login", Map.of("username", "user", "password", "wrong"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("\"message\":\"账号或密码错误\"");
    }

    private Session login(String username, String password) throws Exception {
        ResponseEntity<String> response = post("/auth/login", Map.of("username", username, "password", password));
        assertOk(response);
        JsonNode value = data(response);
        return new Session(
                value.path("token").asText(),
                value.path("userId").asLong(),
                value.path("username").asText(),
                value.path("role").asText()
        );
    }

    private ResponseEntity<String> post(String path, Object body) {
        return post(path, body, null);
    }

    private ResponseEntity<String> post(String path, Object body, String token) {
        return restTemplate.exchange(
                url(path),
                HttpMethod.POST,
                new HttpEntity<>(body, headers(token)),
                String.class
        );
    }

    private ResponseEntity<String> get(String path, String token) {
        return restTemplate.exchange(
                url(path),
                HttpMethod.GET,
                new HttpEntity<>(headers(token)),
                String.class
        );
    }

    private HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null && !token.isBlank()) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    private JsonNode data(ResponseEntity<String> response) throws Exception {
        return objectMapper.readTree(response.getBody()).path("data");
    }

    private String url(String path) {
        return "http://127.0.0.1:" + port + path;
    }

    private static void assertOk(ResponseEntity<String> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"code\":\"OK\"");
    }

    private record Session(String token, long userId, String username, String role) {
    }
}
