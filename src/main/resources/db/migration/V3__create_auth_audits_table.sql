CREATE TABLE auth_audits (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID,
    event_type VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    client_ip VARCHAR(45),
    user_agent TEXT,
    correlation_id VARCHAR(36)
);

CREATE INDEX idx_auth_audits_user_id ON auth_audits(user_id);
CREATE INDEX idx_auth_audits_correlation_id ON auth_audits(correlation_id);
