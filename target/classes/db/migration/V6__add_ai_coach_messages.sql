CREATE TABLE ai_coach_messages (
    message_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    couple_id UUID NOT NULL REFERENCES couples(couple_id),
    sender_id UUID REFERENCES users(user_id),
    role VARCHAR(20) NOT NULL, -- 'USER' or 'ASSISTANT'
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_coach_messages_couple ON ai_coach_messages(couple_id);
