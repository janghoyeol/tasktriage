package com.tasktriage.backend;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 실제 HTTP 요청(MockMvc)으로 회원가입 -> 로그인 -> 인증 -> Task 등록 -> 조회까지 전체 흐름을 검증한다.
 * 서비스 레이어 유닛 테스트(mock)와 달리 Security 필터 체인, JSON 직렬화, 실제 DB까지 전부 탄다.
 *
 * 요청/응답 JSON은 ObjectMapper 대신 문자열/JsonPath로 직접 다룬다. Boot 4는 자체 JacksonAutoConfiguration이
 * Jackson 3(tools.jackson)의 JsonMapper만 자동 등록하는데, 이 프로젝트는 jjwt-jackson이 끌어오는 Jackson 2
 * (com.fasterxml.jackson)만 갖고 있어 com.fasterxml.jackson.databind.ObjectMapper 빈이 없다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TaskApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void fullTaskLifecycleThroughRealHttp() throws Exception {
        String email = "integration-" + System.nanoTime() + "@example.com";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"name":"Integration Test","email":"%s","password":"password123","role":"OWNER"}
                                """
                                        .formatted(email)))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"email":"%s","password":"password123"}
                                """
                                        .formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        String token = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.accessToken");

        mockMvc.perform(get("/api/tasks")).andExpect(status().isForbidden());

        // 규칙 기반(Gate 1)으로 확정 분류되는 문구를 골라서, 외부 Gate 2(FastAPI) 서비스가
        // 이 통합 테스트 실행 환경에 없어도(예: CI) 안정적으로 통과하게 한다.
        MvcResult createResult = mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"title":"Integration test bug","description":"Something is broken and urgent","source":"MANUAL"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        int taskId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/tasks").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Integration test bug")));

        mockMvc.perform(get("/api/tasks/" + taskId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusHistory").exists());
    }

    @Test
    void registerDuplicateEmailReturnsConflict() throws Exception {
        String email = "dup-" + System.nanoTime() + "@example.com";
        String body =
                """
                {"name":"Dup","email":"%s","password":"password123","role":"OWNER"}
                """
                        .formatted(email);

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
        String email = "wrongpass-" + System.nanoTime() + "@example.com";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"name":"Wrong Pass","email":"%s","password":"password123","role":"OWNER"}
                                """
                                        .formatted(email)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"email":"%s","password":"wrongpassword"}
                                """
                                        .formatted(email)))
                .andExpect(status().isUnauthorized());
    }
}
