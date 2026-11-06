-- ============================================================
-- TIENDA AGIL - Base de datos relacional (MySQL 8.0+)
-- Modelo físico alineado con el documento integrador:
-- Usuario, Categoria, Producto, Turno, Venta, DetalleVenta,
-- Pago, Backup y Sincronizacion.
--
-- Nota: este script crea el diseño físico. La aplicación Java
-- actual usa persistencia local serializada; JDBC/MySQL requiere
-- integración posterior.
-- ============================================================

CREATE DATABASE IF NOT EXISTS tienda_agil
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE tienda_agil;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS sincronizacion;
DROP TABLE IF EXISTS backup;
DROP TABLE IF EXISTS pago;
DROP TABLE IF EXISTS detalle_venta;
DROP TABLE IF EXISTS venta;
DROP TABLE IF EXISTS turno;
DROP TABLE IF EXISTS producto;
DROP TABLE IF EXISTS categoria;
DROP TABLE IF EXISTS usuario;

SET FOREIGN_KEY_CHECKS = 1;

-- 1. Usuarios del sistema (administrador / cajero)
CREATE TABLE usuario (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    usuario VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    rol ENUM('ADMINISTRADOR', 'CAJERO') NOT NULL,
    estado TINYINT(1) NOT NULL DEFAULT 1,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2. Categorías del inventario
CREATE TABLE categoria (
    id_categoria INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(255),
    estado TINYINT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB;

-- 3. Productos y control de stock
CREATE TABLE producto (
    id_producto INT AUTO_INCREMENT PRIMARY KEY,
    codigo_barra VARCHAR(50) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(255),
    precio_venta DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    stock_minimo INT NOT NULL DEFAULT 5,
    estado TINYINT(1) NOT NULL DEFAULT 1,
    id_categoria INT NOT NULL,
    CONSTRAINT chk_producto_precio CHECK (precio_venta >= 0),
    CONSTRAINT chk_producto_stock CHECK (stock >= 0),
    CONSTRAINT chk_producto_stock_minimo CHECK (stock_minimo >= 0),
    CONSTRAINT fk_producto_categoria
        FOREIGN KEY (id_categoria) REFERENCES categoria(id_categoria)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- 4. Turnos de caja y arqueo
CREATE TABLE turno (
    id_turno INT AUTO_INCREMENT PRIMARY KEY,
    fecha_inicio DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_fin DATETIME NULL,
    monto_inicial DECIMAL(10,2) NOT NULL DEFAULT 0,
    monto_final DECIMAL(10,2) NULL,
    efectivo_esperado DECIMAL(10,2) NULL,
    diferencia DECIMAL(10,2) NULL,
    estado ENUM('ABIERTO', 'CERRADO') NOT NULL DEFAULT 'ABIERTO',
    id_usuario INT NOT NULL,
    CONSTRAINT chk_turno_monto_inicial CHECK (monto_inicial >= 0),
    CONSTRAINT fk_turno_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- 5. Cabecera de ventas
CREATE TABLE venta (
    id_venta INT AUTO_INCREMENT PRIMARY KEY,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    subtotal DECIMAL(10,2) NOT NULL,
    igv DECIMAL(10,2) NOT NULL DEFAULT 0,
    total DECIMAL(10,2) NOT NULL,
    estado ENUM('REGISTRADA', 'ANULADA') NOT NULL DEFAULT 'REGISTRADA',
    id_usuario INT NOT NULL,
    id_turno INT NOT NULL,
    CONSTRAINT chk_venta_importes CHECK (subtotal >= 0 AND igv >= 0 AND total >= 0),
    CONSTRAINT fk_venta_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_venta_turno
        FOREIGN KEY (id_turno) REFERENCES turno(id_turno)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- 6. Productos incluidos en cada venta
CREATE TABLE detalle_venta (
    id_detalle INT AUTO_INCREMENT PRIMARY KEY,
    id_venta INT NOT NULL,
    id_producto INT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    CONSTRAINT chk_detalle_cantidad CHECK (cantidad > 0),
    CONSTRAINT chk_detalle_importes CHECK (precio_unitario >= 0 AND subtotal >= 0),
    CONSTRAINT fk_detalle_venta
        FOREIGN KEY (id_venta) REFERENCES venta(id_venta)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_detalle_producto
        FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- 7. Medio y monto de pago de la venta
CREATE TABLE pago (
    id_pago INT AUTO_INCREMENT PRIMARY KEY,
    id_venta INT NOT NULL UNIQUE,
    tipo_pago ENUM('EFECTIVO', 'TARJETA', 'QR') NOT NULL,
    monto DECIMAL(10,2) NOT NULL,
    fecha_pago DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_pago_monto CHECK (monto >= 0),
    CONSTRAINT fk_pago_venta
        FOREIGN KEY (id_venta) REFERENCES venta(id_venta)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;

-- 8. Registro de respaldos
CREATE TABLE backup (
    id_backup INT AUTO_INCREMENT PRIMARY KEY,
    fecha_backup DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado ENUM('EXITOSO', 'FALLIDO', 'PENDIENTE') NOT NULL,
    ubicacion VARCHAR(255)
) ENGINE=InnoDB;

-- 9. Registro de sincronizaciones asociadas a respaldos
CREATE TABLE sincronizacion (
    id_sincronizacion INT AUTO_INCREMENT PRIMARY KEY,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado ENUM('PENDIENTE', 'COMPLETADA', 'ERROR') NOT NULL,
    registros_sincronizados INT NOT NULL DEFAULT 0,
    id_backup INT NULL,
    CONSTRAINT chk_sincronizacion_registros CHECK (registros_sincronizados >= 0),
    CONSTRAINT fk_sincronizacion_backup
        FOREIGN KEY (id_backup) REFERENCES backup(id_backup)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB;

-- Índices para consultas frecuentes
CREATE INDEX idx_producto_nombre ON producto(nombre);
CREATE INDEX idx_producto_categoria ON producto(id_categoria);
CREATE INDEX idx_venta_fecha ON venta(fecha);
CREATE INDEX idx_venta_turno ON venta(id_turno);
CREATE INDEX idx_detalle_venta ON detalle_venta(id_venta);
CREATE INDEX idx_detalle_producto ON detalle_venta(id_producto);
CREATE INDEX idx_turno_estado ON turno(estado);
CREATE INDEX idx_sincronizacion_estado ON sincronizacion(estado);

-- Datos iniciales de categorías (opcionales para la demostración)
INSERT INTO categoria (nombre, descripcion) VALUES
('Bebidas', 'Gaseosas, agua y bebidas'),
('Snacks', 'Galletas, papas y productos similares'),
('Lácteos', 'Leche y derivados'),
('Cuidado personal', 'Productos de higiene personal')
ON DUPLICATE KEY UPDATE descripcion = VALUES(descripcion);

-- Productos de demostración. Se insertan solo si el código no existe.
INSERT INTO producto
(codigo_barra, nombre, descripcion, precio_venta, stock, stock_minimo, id_categoria)
SELECT '775000000001', 'Gaseosa Cola 500 ml', 'Bebida gaseosa', 3.50, 50, 10, id_categoria
FROM categoria WHERE nombre = 'Bebidas'
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

INSERT INTO producto
(codigo_barra, nombre, descripcion, precio_venta, stock, stock_minimo, id_categoria)
SELECT '775000000002', 'Papas Clásicas', 'Snack salado', 4.00, 30, 5, id_categoria
FROM categoria WHERE nombre = 'Snacks'
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

INSERT INTO producto
(codigo_barra, nombre, descripcion, precio_venta, stock, stock_minimo, id_categoria)
SELECT '775000000003', 'Leche Entera 1 L', 'Producto lácteo', 5.50, 20, 5, id_categoria
FROM categoria WHERE nombre = 'Lácteos'
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

INSERT INTO producto
(codigo_barra, nombre, descripcion, precio_venta, stock, stock_minimo, id_categoria)
SELECT '775000000004', 'Agua Mineral 625 ml', 'Agua embotellada', 2.00, 100, 20, id_categoria
FROM categoria WHERE nombre = 'Bebidas'
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

INSERT INTO producto
(codigo_barra, nombre, descripcion, precio_venta, stock, stock_minimo, id_categoria)
SELECT '775000000005', 'Galletas de Chocolate', 'Galletas dulces', 3.00, 40, 10, id_categoria
FROM categoria WHERE nombre = 'Snacks'
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

-- No se crea un usuario de demostración porque password_hash debe
-- generarse con el mismo algoritmo PBKDF2 usado por la aplicación.
