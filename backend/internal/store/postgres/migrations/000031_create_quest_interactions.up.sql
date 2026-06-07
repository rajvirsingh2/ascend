-- Phase 5: Quest preference tracking
-- Track every quest shown to user with outcome
CREATE TABLE quest_interactions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id),
    quest_id            UUID NOT NULL REFERENCES quests(id),
    profile_snapshot    JSONB NOT NULL,     -- user profile at time of generation
    quest_data          JSONB NOT NULL,     -- full quest JSON
    shown_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    outcome             TEXT,               -- 'completed' | 'skipped' | 'abandoned' | 'pending'
    outcome_at          TIMESTAMPTZ,
    time_to_outcome     INTERVAL,           -- how long until completion/skip
    model_version       TEXT NOT NULL       -- which model generated this
);

CREATE INDEX idx_qi_user_outcome ON quest_interactions(user_id, outcome);
CREATE INDEX idx_qi_shown_at     ON quest_interactions(shown_at DESC);

-- View: preference pairs ready for DPO training
-- Pairs same-profile completed quests (chosen) with skipped quests (rejected)
CREATE VIEW dpo_preference_pairs AS
SELECT
    c.user_id,
    c.profile_snapshot,
    c.quest_data    AS chosen_quest,
    r.quest_data    AS rejected_quest,
    c.shown_at      AS pair_date
FROM quest_interactions c
JOIN quest_interactions r
    ON c.user_id = r.user_id
   AND c.profile_snapshot = r.profile_snapshot
   AND c.shown_at::DATE = r.shown_at::DATE
WHERE c.outcome = 'completed'
  AND r.outcome IN ('skipped', 'abandoned');
