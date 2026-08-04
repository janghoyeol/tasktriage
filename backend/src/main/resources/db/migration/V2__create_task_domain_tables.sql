CREATE TABLE tasks (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    description VARCHAR(5000),
    category    VARCHAR(30),
    urgency     VARCHAR(10),
    status      VARCHAR(20) NOT NULL,
    source      VARCHAR(10) NOT NULL,
    due_at      TIMESTAMP,
    owner_id    BIGINT NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL
);

CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_owner_id ON tasks(owner_id);

CREATE TABLE triage_logs (
    id               BIGSERIAL PRIMARY KEY,
    task_id          BIGINT NOT NULL REFERENCES tasks(id),
    gate             VARCHAR(20) NOT NULL,
    llm_called       BOOLEAN NOT NULL,
    confidence_score DOUBLE PRECISION,
    result_category  VARCHAR(30),
    result_urgency   VARCHAR(10),
    created_at       TIMESTAMP NOT NULL
);

CREATE INDEX idx_triage_logs_task_id ON triage_logs(task_id);

CREATE TABLE task_status_histories (
    id          BIGSERIAL PRIMARY KEY,
    task_id     BIGINT NOT NULL REFERENCES tasks(id),
    from_status VARCHAR(20),
    to_status   VARCHAR(20) NOT NULL,
    changed_at  TIMESTAMP NOT NULL
);

CREATE INDEX idx_task_status_histories_task_id ON task_status_histories(task_id);

CREATE TABLE sla_rules (
    id                  BIGSERIAL PRIMARY KEY,
    urgency             VARCHAR(10) NOT NULL UNIQUE,
    response_time_hours INTEGER NOT NULL
);

INSERT INTO sla_rules (urgency, response_time_hours) VALUES
    ('LOW', 72),
    ('MEDIUM', 24),
    ('HIGH', 8),
    ('URGENT', 2);
