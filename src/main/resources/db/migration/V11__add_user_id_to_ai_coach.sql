ALTER TABLE ai_coach_messages ADD COLUMN user_id UUID;

-- Populate existing USER messages as a best-effort
UPDATE ai_coach_messages SET user_id = sender_id WHERE role = 'USER' AND user_id IS NULL;

-- Create index for performance
CREATE INDEX idx_ai_coach_messages_user ON ai_coach_messages(user_id);
