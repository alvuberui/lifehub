-- Conversión in-place: conserva todas las filas y evita la aritmética binaria de DOUBLE PRECISION.
ALTER TABLE users
    ALTER COLUMN initial_wealth TYPE NUMERIC USING initial_wealth::NUMERIC,
    ALTER COLUMN monthly_saving_percentage TYPE NUMERIC USING monthly_saving_percentage::NUMERIC,
    ALTER COLUMN monthly_necessary_expenses_percentage TYPE NUMERIC USING monthly_necessary_expenses_percentage::NUMERIC,
    ALTER COLUMN monthly_discretionary_expenses_percentage TYPE NUMERIC USING monthly_discretionary_expenses_percentage::NUMERIC,
    ALTER COLUMN monthly_investment_percentage TYPE NUMERIC USING monthly_investment_percentage::NUMERIC;
