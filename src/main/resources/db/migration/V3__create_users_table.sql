CREATE TABLE users (
    id         UUID PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    birth_date DATE         NOT NULL,
    email      VARCHAR(255) NOT NULL,
    address    VARCHAR(500),
    user_type  VARCHAR(20)  NOT NULL CHECK (user_type IN ('BUSINESS', 'INDIVIDUAL')),
    created_at TIMESTAMP    NOT NULL
);

CREATE UNIQUE INDEX uq_users_email ON users (lower(email));
