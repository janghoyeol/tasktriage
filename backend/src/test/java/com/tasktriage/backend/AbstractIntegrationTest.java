package com.tasktriage.backend;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @SpringBootTest를 쓰는 테스트가 로컬 docker-compose Postgres에 의존하지 않도록,
 * 테스트 시작 시 진짜 Postgres 컨테이너를 띄우고 끝나면 정리한다. CI(GitHub Actions)에서도
 * Docker만 있으면 별도 설정 없이 그대로 동작한다.
 */
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
}
