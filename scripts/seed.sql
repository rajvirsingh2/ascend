BEGIN;

-- ── Clean existing test data ──────────────────────────────────────────
DELETE FROM user_skills     WHERE user_id IN (SELECT id FROM users WHERE email = 'test@ascend.app');
DELETE FROM quests          WHERE user_id IN (SELECT id FROM users WHERE email = 'test@ascend.app');
DELETE FROM habits          WHERE user_id IN (SELECT id FROM users WHERE email = 'test@ascend.app');
DELETE FROM goals           WHERE user_id IN (SELECT id FROM users WHERE email = 'test@ascend.app');
DELETE FROM users           WHERE email = 'test@ascend.app';

-- ── Test user (password: "[PASSWORD]") ───────────────────────────────
-- bcrypt hash at cost 12 for "password123"
INSERT INTO users (id, email, password_hash, username, level, total_xp, current_xp, hp, max_hp, email_verified, email_verified_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'test@ascend.app',
    '$2b$12$sg/cNMsGjsYLVM74KazwOubHlEGEbC/R1w6D06a2NJgaSCJ3pUZW6',
    'TestHero',
    3,
    450,
    50,
    87,
    100,
    true,
    NOW()
);

-- ── Goals ─────────────────────────────────────────────────────────────
INSERT INTO goals (id, user_id, title, skill_area, priority, status, progress)
VALUES
    ('00000000-0000-0000-0000-000000000010',
     '00000000-0000-0000-0000-000000000001',
     'Run a 5K without stopping', 'fitness', 2, 'active', 30),
    ('00000000-0000-0000-0000-000000000011',
     '00000000-0000-0000-0000-000000000001',
     'Read 12 books this year', 'learning', 1, 'active', 25)
ON CONFLICT DO NOTHING;

-- ── Habits ────────────────────────────────────────────────────────────
INSERT INTO habits (id, user_id, goal_id, title, frequency, xp_reward, current_streak)
VALUES
    ('00000000-0000-0000-0000-000000000020',
     '00000000-0000-0000-0000-000000000001',
     '00000000-0000-0000-0000-000000000010',
     'Morning run 20 minutes', 'daily', 15, 5),
    ('00000000-0000-0000-0000-000000000021',
     '00000000-0000-0000-0000-000000000001',
     '00000000-0000-0000-0000-000000000011',
     'Read 20 pages before bed', 'daily', 10, 3)
ON CONFLICT DO NOTHING;

-- ── Quests ────────────────────────────────────────────────────────────
INSERT INTO quests (id, user_id, goal_id, title, description, type, difficulty, xp_reward, status, skill_area)
VALUES
    ('00000000-0000-0000-0000-000000000030',
     '00000000-0000-0000-0000-000000000001',
     '00000000-0000-0000-0000-000000000010',
     'Complete a 2km run',
     'Head outside and complete a 2km run at any pace. Focus on finishing, not speed.',
     'daily', 2, 40, 'active', 'fitness'),
    ('00000000-0000-0000-0000-000000000031',
     '00000000-0000-0000-0000-000000000001',
     '00000000-0000-0000-0000-000000000011',
     'Read for 30 minutes straight',
     'Find a quiet spot and read your current book for 30 uninterrupted minutes.',
     'daily', 1, 25, 'active', 'learning')
ON CONFLICT DO NOTHING;

-- ── User skills ───────────────────────────────────────────────────────
INSERT INTO user_skills (user_id, skill_name, skill_level, xp_in_skill)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'fitness', 2, 120),
    ('00000000-0000-0000-0000-000000000001', 'learning', 1, 80)
ON CONFLICT DO NOTHING;

COMMIT;