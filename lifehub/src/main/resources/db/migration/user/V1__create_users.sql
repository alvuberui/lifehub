CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    lastname VARCHAR(255) NOT NULL,
    birthdate VARCHAR(255) NOT NULL,
    initial_wealth NUMERIC NOT NULL,
    monthly_saving_percentage NUMERIC NOT NULL,
    monthly_necessary_expenses_percentage NUMERIC NOT NULL,
    monthly_discretionary_expenses_percentage NUMERIC NOT NULL,
    monthly_investment_percentage NUMERIC NOT NULL
);
