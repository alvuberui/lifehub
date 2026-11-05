-- Conversión in-place: conserva todas las filas y evita la aritmética binaria de DOUBLE PRECISION.
ALTER TABLE movements
    ALTER COLUMN amount TYPE NUMERIC USING amount::NUMERIC;
