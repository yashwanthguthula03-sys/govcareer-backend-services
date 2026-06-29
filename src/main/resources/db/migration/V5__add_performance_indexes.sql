-- V5: Add Performance Indexes for Production Scalability

-- 1. Index on refresh_tokens.user_id to prevent full table scans on logout
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- 2. Index on email_verification_tokens.user_id to prevent full table scans during resend/login
CREATE INDEX idx_email_tokens_user_id ON email_verification_tokens(user_id);

-- 3. Index on password_reset_tokens.user_id to prevent full table scans on forgot-password
CREATE INDEX idx_reset_tokens_user_id ON password_reset_tokens(user_id);

-- 4. Compound index on auth_audits for efficient time-series lookup per user
CREATE INDEX idx_auth_audits_user_time ON auth_audits(user_id, timestamp);
