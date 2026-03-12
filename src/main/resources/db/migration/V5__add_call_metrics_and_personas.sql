-- Migration to add Partner Persona table and enhance Relationship Metrics

CREATE TABLE partner_personas (
    persona_id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID NOT NULL UNIQUE REFERENCES users(user_id) ON DELETE CASCADE,
    couple_id            UUID NOT NULL REFERENCES couples(couple_id) ON DELETE CASCADE,
    communication_style  VARCHAR(100),
    love_language_primary VARCHAR(100),
    aura                VARCHAR(100),
    traits              TEXT,
    updated_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_partner_personas_couple_id ON partner_personas(couple_id);

ALTER TABLE relationship_metrics 
ADD COLUMN call_count INTEGER NOT NULL DEFAULT 0,
ADD COLUMN total_call_minutes INTEGER NOT NULL DEFAULT 0;
