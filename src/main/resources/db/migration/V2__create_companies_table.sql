CREATE TABLE companies (
    id   UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE UNIQUE INDEX uq_companies_name ON companies (lower(name));
