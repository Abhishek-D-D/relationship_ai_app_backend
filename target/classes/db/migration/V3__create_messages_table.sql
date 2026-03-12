CREATE TYPE message_type_enum AS ENUM ('TEXT', 'IMAGE', 'AUDIO', 'VIDEO', 'STICKER');

CREATE TABLE messages (
    message_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    couple_id     UUID NOT NULL REFERENCES couples(couple_id) ON DELETE CASCADE,
    sender_id     UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    message_type  message_type_enum NOT NULL DEFAULT 'TEXT',
    content       TEXT,
    media_url     TEXT,
    is_read       BOOLEAN NOT NULL DEFAULT FALSE,
    read_at       TIMESTAMP WITHOUT TIME ZONE,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_messages_couple_id         ON messages(couple_id);
CREATE INDEX idx_messages_sender_id         ON messages(sender_id);
CREATE INDEX idx_messages_couple_created_at ON messages(couple_id, created_at DESC);
