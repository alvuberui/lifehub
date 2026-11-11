-- Cada miembro de una conexión puede compartir un permiso distinto con el otro.
-- La fila de Álvaro no debe sobrescribirse cuando Cristina modifica su permiso.
ALTER TABLE connections.module_permissions
    DROP CONSTRAINT IF EXISTS module_permissions_unique_module;

ALTER TABLE connections.module_permissions
    ADD CONSTRAINT module_permissions_unique_direction
        UNIQUE (connection_id, module_key, granted_by_email);
