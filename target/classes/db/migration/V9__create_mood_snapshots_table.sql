-- Feature 2: Mood snapshots table (12-hour sentiment analysis)
CREATE TABLE mood_snapshots (
    snapshot_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    couple_id         UUID NOT NULL REFERENCES couples(couple_id) ON DELETE CASCADE,
    user_id           UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    mood              VARCHAR(30) NOT NULL DEFAULT 'NEUTRAL',
    mood_score        INT NOT NULL DEFAULT 50,
    mood_note         TEXT,
    notification_sent BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_mood_couple_user_time ON mood_snapshots(couple_id, user_id, created_at DESC);
