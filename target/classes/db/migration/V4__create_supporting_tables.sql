CREATE TYPE call_type_enum   AS ENUM ('VOICE', 'VIDEO');
CREATE TYPE call_status_enum AS ENUM ('COMPLETED', 'MISSED', 'DECLINED', 'FAILED');

CREATE TABLE call_logs (
    call_id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    couple_id        UUID NOT NULL REFERENCES couples(couple_id) ON DELETE CASCADE,
    initiator_id     UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    call_type        call_type_enum NOT NULL,
    duration_seconds INTEGER NOT NULL DEFAULT 0,
    status           call_status_enum NOT NULL DEFAULT 'COMPLETED',
    started_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    ended_at         TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX idx_call_logs_couple_id  ON call_logs(couple_id);
CREATE INDEX idx_call_logs_started_at ON call_logs(started_at DESC);

CREATE TABLE ai_insights (
    insight_id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    couple_id    UUID NOT NULL REFERENCES couples(couple_id) ON DELETE CASCADE,
    week_start   DATE NOT NULL,
    health_score SMALLINT NOT NULL DEFAULT 50 CHECK (health_score >= 0 AND health_score <= 100),
    summary      TEXT,
    suggestions  TEXT,
    generated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(couple_id, week_start)
);

CREATE INDEX idx_ai_insights_couple_id  ON ai_insights(couple_id);
CREATE INDEX idx_ai_insights_week_start ON ai_insights(week_start DESC);

CREATE TABLE relationship_metrics (
    metric_id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    couple_id              UUID NOT NULL REFERENCES couples(couple_id) ON DELETE CASCADE,
    period_start           DATE NOT NULL,
    period_end             DATE NOT NULL,
    message_count          INTEGER NOT NULL DEFAULT 0,
    avg_response_time_mins NUMERIC(8,2) NOT NULL DEFAULT 0,
    conflict_score         SMALLINT NOT NULL DEFAULT 0,
    positive_score         SMALLINT NOT NULL DEFAULT 50,
    computed_at            TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(couple_id, period_start)
);

CREATE INDEX idx_rel_metrics_couple_id  ON relationship_metrics(couple_id);
