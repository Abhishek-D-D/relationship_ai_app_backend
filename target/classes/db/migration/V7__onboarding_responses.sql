CREATE TABLE onboarding_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_text TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,
    options JSONB, -- Array of strings for multiple choice
    weight INTEGER DEFAULT 1
);

CREATE TABLE onboarding_responses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(user_id),
    couple_id UUID NOT NULL REFERENCES couples(couple_id),
    question_id UUID NOT NULL REFERENCES onboarding_questions(id),
    answer_text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Seed initial "Deep Vibe" questions
INSERT INTO onboarding_questions (question_text, category, options) VALUES 
('When we disagree, my first instinct is to...', 'CONFLICT', '["Withdraw and go silent", "Explain my side immediately", "Defend myself", "Try to soften the mood"]'),
('I feel most connected to my partner when they...', 'LOVE_LANGUAGE', '["Take something off my plate (Acts)", "Give me a thoughtful gift (Gifts)", "Really listen to me (Words)", "Initiate physical touch (Physical)"]'),
('The most important thing for me in our relationship right now is...', 'PRIORITY', '["Having more fun together", "Building deeper emotional trust", "Better practical communication", "Planning for our future"]'),
('A perfect weekend morning for me involves...', 'PERSONALITY', '["An early outdoor adventure", "A quiet coffee and book", "Sleeping in as late as possible", "A big brunch with friends"]');
