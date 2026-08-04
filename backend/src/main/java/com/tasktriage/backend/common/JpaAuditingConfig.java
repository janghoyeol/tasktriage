package com.tasktriage.backend.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * @CreatedDate/@LastModifiedDate가 동작하려면 이 설정이 필요하다.
 * created_at/updated_at을 매번 서비스 코드에서 수동으로 채우지 않아도 되게 해준다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
