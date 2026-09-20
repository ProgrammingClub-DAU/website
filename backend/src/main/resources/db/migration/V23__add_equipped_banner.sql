-- V23: Add equipped_banner_id and max_rating to users table for leaderboard banner system

ALTER TABLE users ADD COLUMN IF NOT EXISTS equipped_banner_id VARCHAR(50) DEFAULT 'rookie';
ALTER TABLE users ADD COLUMN IF NOT EXISTS max_rating INT;
