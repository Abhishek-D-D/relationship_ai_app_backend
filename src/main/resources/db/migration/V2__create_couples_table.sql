CREATE TABLE couples (
    couple_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partner1_id      UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    partner2_id      UUID REFERENCES users(user_id) ON DELETE SET NULL,
    invite_code      VARCHAR(12) NOT NULL UNIQUE,
    anniversary_date DATE,
    created_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_couples_partner1    ON couples(partner1_id);
CREATE INDEX idx_couples_partner2    ON couples(partner2_id);
CREATE INDEX idx_couples_invite_code ON couples(invite_code);
