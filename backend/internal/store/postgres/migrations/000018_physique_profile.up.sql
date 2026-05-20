CREATE TABLE physique_profiles (
    user_id          UUID        PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    age              INTEGER     NOT NULL CHECK (age BETWEEN 10 AND 100),
    sex              VARCHAR(10) NOT NULL CHECK (sex IN ('male','female','other')),
    height_cm        NUMERIC(5,1) NOT NULL,
    weight_kg        NUMERIC(5,1) NOT NULL,
    target_weight_kg NUMERIC(5,1),
    body_goal        VARCHAR(20) NOT NULL
                         CHECK (body_goal IN (
                             'lean_athletic',
                             'bulky_muscular',
                             'powerlifter',
                             'endurance',
                             'maintain',
                             'lose_fat'
                         )),
    activity_level   VARCHAR(20) NOT NULL
                         CHECK (activity_level IN (
                             'sedentary',
                             'light',
                             'moderate',
                             'active',
                             'very_active'
                         )),
    fitness_level    VARCHAR(20) NOT NULL DEFAULT 'beginner'
                         CHECK (fitness_level IN ('beginner','intermediate','advanced')),
    bmi              NUMERIC(4,1),
    bmr              INTEGER,
    tdee             INTEGER,
    computed_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE exercise_completions (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    quest_id     UUID        REFERENCES quests(id) ON DELETE SET NULL,
    exercise_key VARCHAR(50) NOT NULL,
    sets_done    INTEGER,
    reps_done    INTEGER,
    duration_sec INTEGER,
    weight_kg    NUMERIC(5,1),
    notes        TEXT,
    completed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_physique_user  ON physique_profiles (user_id);
CREATE INDEX idx_exercise_user  ON exercise_completions (user_id, completed_at DESC);