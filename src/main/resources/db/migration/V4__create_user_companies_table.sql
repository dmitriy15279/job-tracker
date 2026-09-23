CREATE TABLE user_companies (
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, company_id)
);

CREATE INDEX idx_user_companies_company_id ON user_companies (company_id);
