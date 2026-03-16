-- Feature 1: Relationship Timeline milestones table
CREATE TABLE relationship_milestones (
    milestone_id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    couple_id      UUID NOT NULL REFERENCES couples(couple_id) ON DELETE CASCADE,
    title          TEXT NOT NULL,
    memory_summary TEXT,
    milestone_date DATE NOT NULL,
    milestone_type VARCHAR(50) NOT NULL DEFAULT 'MOMENT',
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_milestones_couple_date ON relationship_milestones(couple_id, milestone_date ASC);
