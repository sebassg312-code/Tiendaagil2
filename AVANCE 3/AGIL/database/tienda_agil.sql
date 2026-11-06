CREATE DATABASE IF NOT EXISTS tienda_agil CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE tienda_agil;

CREATE TABLE IF NOT EXISTS usuario (
  id_usuario INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL,
  usuario VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  rol ENUM('ADMINISTRADOR','CAJERO') NOT NULL,
  estado TINYINT(1) NOT NULL DEFAULT 1,
  fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS categoria (
  id_categoria INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL UNIQUE,
  descripcion VARCHAR(255),
  estado TINYINT(1) NOT NULL DEFAULT 1
);
CREATE TABLE IF NOT EXISTS producto (
  id_producto INT AUTO_INCREMENT PRIMARY KEY,
  codigo_barra VARCHAR(50) NOT NULL UNIQUE,
  nombre VARCHAR(150) NOT NULL,
  descripcion VARCHAR(255),
  precio_venta DECIMAL(10,2) NOT NULL,
  stock INT NOT NULL DEFAULT 0,
  stock_minimo INT NOT NULL DEFAULT 5,
  estado TINYINT(1) NOT NULL DEFAULT 1,
  id_categoria INT NOT NULL,
  CONSTRAINT fk_producto_categoria FOREIGN KEY (id_categoria) REFERENCES categoria(id_categoria)
);
CREATE TABLE IF NOT EXISTS turno (
  id_turno INT AUTO_INCREMENT PRIMARY KEY,
  fecha_inicio DATETIME NOT NULL,
  fecha_fin DATETIME NULL,
  monto_inicial DECIMAL(10,2) NOT NULL DEFAULT 0,
  monto_final DECIMAL(10,2) NULL,
  efectivo_esperado DECIMAL(10,2) NULL,
  diferencia DECIMAL(10,2) NULL,
  estado ENUM('ABIERTO','CERRADO') NOT NULL DEFAULT 'ABIERTO',
  id_usuario INT NOT NULL,
  CONSTRAINT fk_turno_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
);
CREATE TABLE IF NOT EXISTS venta (
  id_venta INT AUTO_INCREMENT PRIMARY KEY,
  fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  subtotal DECIMAL(10,2) NOT NULL,
  igv DECIMAL(10,2) NOT NULL DEFAULT 0,
  total DECIMAL(10,2) NOT NULL,
  estado ENUM('REGISTRADA','ANULADA') NOT NULL DEFAULT 'REGISTRADA',
  id_usuario INT NOT NULL,
  id_turno INT NOT NULL,
  CONSTRAINT fk_venta_usuario FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario),
  CONSTRAINT fk_venta_turno FOREIGN KEY (id_turno) REFERENCES turno(id_turno)
);
CREATE TABLE IF NOT EXISTS detalle_venta (
  id_detalle INT AUTO_INCREMENT PRIMARY KEY,
  id_venta INT NOT NULL,
  id_producto INT NOT NULL,
  cantidad INT NOT NULL,
  precio_unitario DECIMAL(10,2) NOT NULL,
  subtotal DECIMAL(10,2) NOT NULL,
  CONSTRAINT fk_detalle_venta FOREIGN KEY (id_venta) REFERENCES venta(id_venta),
  CONSTRAINT fk_detalle_producto FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
);
CREATE TABLE IF NOT EXISTS pago (
  id_pago INT AUTO_INCREMENT PRIMARY KEY,
  id_venta INT NOT NULL UNIQUE,
  tipo_pago ENUM('EFECTIVO','TARJETA','QR') NOT NULL,
  monto DECIMAL(10,2) NOT NULL,
  fecha_pago DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_pago_venta FOREIGN KEY (id_venta) REFERENCES venta(id_venta)
);
CREATE TABLE IF NOT EXISTS backup (
  id_backup INT AUTO_INCREMENT PRIMARY KEY,
  fecha_backup DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  estado ENUM('EXITOSO','FALLIDO','PENDIENTE') NOT NULL,
  ubicacion VARCHAR(255)
);
CREATE TABLE IF NOT EXISTS sincronizacion (
  id_sincronizacion INT AUTO_INCREMENT PRIMARY KEY,
  fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  estado ENUM('PENDIENTE','COMPLETADA','ERROR') NOT NULL,
  registros_sincronizados INT NOT NULL DEFAULT 0,
  id_backup INT NULL,
  CONSTRAINT fk_sincronizacion_backup FOREIGN KEY (id_backup) REFERENCES backup(id_backup)
);
CREATE INDEX idx_producto_nombre ON producto(nombre);
CREATE INDEX idx_venta_fecha ON venta(fecha);
CREATE INDEX idx_detalle_venta ON detalle_venta(id_venta);
CREATE INDEX idx_detalle_producto ON detalle_venta(id_producto);
