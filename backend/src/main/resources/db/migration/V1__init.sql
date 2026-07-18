-- WP1-T3:初始 schema(規劃書 §6)。
-- conversations / messages / artifacts / providers / agent_profiles / audit_logs

CREATE TABLE conversations (
    id               UUID PRIMARY KEY,
    title            TEXT        NOT NULL,
    system_prompt    TEXT,
    default_model_id TEXT,
    temperature      DOUBLE PRECISION,
    agent_profile_id UUID,
    created_by       TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE messages (
    id                    UUID PRIMARY KEY,
    conversation_id       UUID        NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    seq                   INT         NOT NULL,
    role                  TEXT        NOT NULL CHECK (role IN ('SYSTEM', 'USER', 'ASSISTANT')),
    model_id              TEXT,
    agent_profile_id      UUID,
    agent_profile_version INT,
    content               TEXT        NOT NULL,
    thinking              TEXT,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (conversation_id, seq)
);
CREATE INDEX idx_messages_conversation ON messages (conversation_id);

-- 產出物(ADR-005:獨立資料表並版本化)
CREATE TABLE artifacts (
    id                    UUID PRIMARY KEY,
    message_id            UUID        NOT NULL REFERENCES messages (id) ON DELETE CASCADE,
    agent_profile_version INT,
    type                  TEXT        NOT NULL CHECK (type IN ('GHERKIN', 'JAVA', 'MARKDOWN', 'DOCX')),
    language              TEXT,
    content               TEXT,
    storage_key           TEXT,
    version               INT         NOT NULL DEFAULT 1,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_artifacts_message ON artifacts (message_id);

CREATE TABLE providers (
    id          TEXT PRIMARY KEY,
    type        TEXT    NOT NULL CHECK (type IN ('OLLAMA', 'OPENAI_COMPATIBLE', 'ANTHROPIC')),
    base_url    TEXT    NOT NULL,
    api_key_ref TEXT,
    enabled     BOOLEAN NOT NULL DEFAULT TRUE
);

-- Agent Profile(ADR-006:append-only 版本化;主鍵為 (id, version))
CREATE TABLE agent_profiles (
    id               UUID        NOT NULL,
    version          INT         NOT NULL,
    name             TEXT        NOT NULL,
    description      TEXT,
    system_prompt    TEXT        NOT NULL,
    default_model_id TEXT,
    temperature      DOUBLE PRECISION,
    tools            TEXT,
    enabled          BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (id, version)
);

CREATE TABLE audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id UUID,
    level           TEXT        NOT NULL,
    source          TEXT,
    payload         TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_conversation ON audit_logs (conversation_id);
