BEGIN;

-- test user (password: "password123")
-- bcrypt hash generated at cost 12
INSERT INTO users (id, email, password_hash, username, level, total_xp, current_xp)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'test@ascend.app',
    '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    'testuser',
    3,
    450,
    50
) ON CONFLICT DO NOTHING;

-- goals for test user
INSERT INTO goals (id, user_id, title, skill_area, priority, status, progress)
VALUES
    ('00000000-0000-0000-0000-000000000010',
     '00000000-0000-0000-0000-000000000001',
     'Run a 5K without stopping', 'fitness', 2, 'active', 30),
    ('00000000-0000-0000-0000-000000000011',
     '00000000-0000-0000-0000-000000000001',
     'Read 12 books this year', 'learning', 1, 'active', 25)
ON CONFLICT DO NOTHING;

-- habits for test user
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

-- quests for test user
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

-- user skills
INSERT INTO user_skills (user_id, skill_name, skill_level, xp_in_skill)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'fitness', 2, 120),
    ('00000000-0000-0000-0000-000000000001', 'learning', 1, 80)
ON CONFLICT DO NOTHING;

COMMIT;