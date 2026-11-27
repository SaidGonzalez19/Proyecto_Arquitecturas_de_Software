-- Ejecutar en MySQL 8.4 después de 00-crear-base.sql.
-- La estructura coincide con la entidad Producto (ddl-auto=none).
USE gestor_productos;

CREATE TABLE IF NOT EXISTS productos (
    id     BIGINT         NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100)   NOT NULL,
    precio DECIMAL(10,2)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_productos_precio CHECK (precio >= 0)
);
