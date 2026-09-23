CREATE TABLE referral_codes (
    code       VARCHAR(16) PRIMARY KEY,
    company_id UUID        NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    created_at TIMESTAMP   NOT NULL,
    expires_at TIMESTAMP   NOT NULL
);

CREATE INDEX idx_referral_codes_expires_at ON referral_codes (expires_at);
