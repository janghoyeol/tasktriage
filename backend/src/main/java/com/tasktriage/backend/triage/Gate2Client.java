package com.tasktriage.backend.triage;

import com.tasktriage.backend.task.Category;
import com.tasktriage.backend.task.Urgency;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Gate 2: triage-service(FastAPI)의 /classify를 호출하는 클라이언트. */
@Component
public class Gate2Client {

    public record ClassifyRequest(String title, String description) {
    }

    public record ClassifyResponse(Category category, Urgency urgency, double confidence, String reasoning) {
    }

    private final RestClient restClient;

    public Gate2Client(@Value("${app.triage.service-url}") String serviceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(serviceUrl)
                // RestClient의 기본 요청 팩토리(JDK HttpClient)로 uvicorn에 POST하면
                // Content-Length는 맞게 계산되는데 서버가 빈 바디로 받는 문제가 있었다
                // (uvicorn과의 HTTP/1.1 처리 방식 차이로 추정). SimpleClientHttpRequestFactory
                // (HttpURLConnection 기반)로 바꾸니 정상 동작해서 이걸로 고정한다.
                .requestFactory(new SimpleClientHttpRequestFactory())
                .build();
    }

    public ClassifyResponse classify(String title, String description) {
        return restClient
                .post()
                .uri("/classify")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ClassifyRequest(title, description))
                .retrieve()
                .body(ClassifyResponse.class);
    }
}
