CREATE DATABASE IF NOT EXISTS ensupunto_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ensupunto_db;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS modificaciones_pedido;
DROP TABLE IF EXISTS pagos_fraccionados;
DROP TABLE IF EXISTS detalles_pedido;
DROP TABLE IF EXISTS pedidos;
DROP TABLE IF EXISTS mesas;
DROP TABLE IF EXISTS platos;
DROP TABLE IF EXISTS usuarios;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    nombre_usuario VARCHAR(50) NOT NULL UNIQUE,
    contrasena VARCHAR(255) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE platos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    categoria VARCHAR(30) NOT NULL,
    precio DECIMAL(10, 2) NOT NULL,
    descripcion TEXT,
    activo BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE mesas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    estado VARCHAR(30) NOT NULL DEFAULT 'libre'
) ENGINE=InnoDB;

CREATE TABLE pedidos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    mesa_id INT NOT NULL,
    mesero_id INT NOT NULL,
    cajero_id INT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'pendiente',
    total DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    metodo_pago VARCHAR(30) NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_pago TIMESTAMP NULL,
    CONSTRAINT fk_pedidos_mesa FOREIGN KEY (mesa_id) REFERENCES mesas(id),
    CONSTRAINT fk_pedidos_mesero FOREIGN KEY (mesero_id) REFERENCES usuarios(id),
    CONSTRAINT fk_pedidos_cajero FOREIGN KEY (cajero_id) REFERENCES usuarios(id)
) ENGINE=InnoDB;

CREATE TABLE detalles_pedido (
    id INT AUTO_INCREMENT PRIMARY KEY,
    pedido_id INT NOT NULL,
    plato_id INT NOT NULL,
    cantidad INT NOT NULL DEFAULT 1,
    precio_unitario DECIMAL(10, 2) NOT NULL,
    nota VARCHAR(255) NULL,
    CONSTRAINT fk_detalles_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE,
    CONSTRAINT fk_detalles_plato FOREIGN KEY (plato_id) REFERENCES platos(id)
) ENGINE=InnoDB;

CREATE TABLE pagos_fraccionados (
    id INT AUTO_INCREMENT PRIMARY KEY,
    pedido_id INT NOT NULL,
    numero_cliente INT NOT NULL,
    monto DECIMAL(10, 2) NOT NULL,
    metodo_pago VARCHAR(30) NULL,
    numero_boleta VARCHAR(30) NULL,
    pagado BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_pago TIMESTAMP NULL,
    CONSTRAINT fk_pagos_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE modificaciones_pedido (
    id INT AUTO_INCREMENT PRIMARY KEY,
    pedido_id INT NOT NULL,
    administrador_id INT NOT NULL,
    detalle VARCHAR(255) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mod_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE,
    CONSTRAINT fk_mod_admin FOREIGN KEY (administrador_id) REFERENCES usuarios(id)
) ENGINE=InnoDB;

CREATE INDEX idx_pedidos_estado ON pedidos(estado);
CREATE INDEX idx_pedidos_fecha ON pedidos(fecha_creacion);
CREATE INDEX idx_platos_categoria ON platos(categoria);
CREATE INDEX idx_usuarios_rol ON usuarios(rol);

INSERT INTO usuarios (id, nombre, rol, nombre_usuario, contrasena, activo) VALUES
(1, 'Juan Pérez', 'mesero', 'mesero', '123', TRUE),
(2, 'Carlos Ruiz', 'chef', 'chef', '123', TRUE),
(3, 'María Gómez', 'cajero', 'cajero', '123', TRUE),
(4, 'Ana Martínez', 'admin', 'admin', '123', TRUE);

INSERT INTO platos (id, nombre, categoria, precio, descripcion, activo) VALUES
(1, 'Ceviche Carretillero', 'entradas', 25.00, 'Pescado fresco marinado en limón con cebolla, camote y choclo, coronado con chicharrón de pota.', TRUE),
(2, 'Papa a la Huancaína', 'entradas', 15.00, 'Papas sancochadas bañadas en salsa cremosa de ají amarillo, queso y leche.', TRUE),
(3, 'Causa Rellena de Pollo', 'entradas', 18.00, 'Masa de papa amarilla sazonada con ají amarillo y limón, rellena de pollo, mayonesa y palta.', TRUE),
(4, 'Lomo Saltado', 'segundos', 38.00, 'Trozos de lomo de res salteados al wok con cebolla, tomate, ají amarillo, servido con papas fritas y arroz.', TRUE),
(5, 'Ají de Gallina', 'segundos', 28.00, 'Pechuga de pollo deshilachada en crema de ají amarillo, leche y nueces, acompañado de arroz y papa.', TRUE),
(6, 'Arroz con Mariscos', 'segundos', 35.00, 'Arroz sazonado con aderezo norteño y mariscos de estación (langostinos, calamar y conchas).', TRUE),
(7, 'Cabrito a la Norteña', 'segundos', 42.00, 'Tierno cabrito macerado en chicha de jora y culantro, servido con frejoles, yuca y arroz.', TRUE),
(8, 'Chicha Morada 1L', 'bebidas', 15.00, 'Bebida tradicional de maíz morado hervido con piña, manzana, canela y clavo de olor.', TRUE),
(9, 'Limonada Frozen', 'bebidas', 12.00, 'Limonada refrescante batida con hielo picado.', TRUE),
(10, 'Gaseosa Personal', 'bebidas', 6.00, 'Coca Cola o Inka Cola personal (500ml).', TRUE),
(11, 'Cerveza Pilsen', 'bebidas', 10.00, 'Cerveza nacional en botella personal.', TRUE),
(12, 'Suspiro a la Limeña', 'postres', 12.00, 'Crema de manjarblanco y yemas, coronado con merengue al oporto.', TRUE),
(13, 'Torta Tres Leches', 'postres', 10.00, 'Bizcochuelo bañado en tres tipos de leche, decorado con chantilly y canela.', TRUE),
(14, 'Crema Volteada', 'postres', 8.00, 'Clásico flan de leche condensada con caramelo líquido.', TRUE);

INSERT INTO mesas (id, nombre, estado) VALUES
(1, 'Mesa 1', 'libre'),
(2, 'Mesa 2', 'libre'),
(3, 'Mesa 3', 'libre'),
(4, 'Mesa 4', 'libre'),
(5, 'Mesa 5', 'libre'),
(6, 'Mesa 6', 'libre'),
(7, 'Mesa 7', 'libre'),
(8, 'Mesa 8', 'libre');

INSERT INTO pedidos (id, mesa_id, mesero_id, cajero_id, estado, total, metodo_pago, fecha_creacion, fecha_pago) VALUES
(1, 2, 1, 3, 'pagado', 78.00, 'efectivo', DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 105 MINUTE));

INSERT INTO detalles_pedido (pedido_id, plato_id, cantidad, precio_unitario, nota) VALUES
(1, 1, 2, 25.00, 'Uno sin ají'),
(1, 8, 1, 15.00, NULL),
(1, 12, 1, 12.00, NULL);

INSERT INTO pedidos (id, mesa_id, mesero_id, cajero_id, estado, total, metodo_pago, fecha_creacion, fecha_pago) VALUES
(2, 5, 1, 3, 'pagado', 84.00, 'tarjeta', DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 40 MINUTE));

INSERT INTO detalles_pedido (pedido_id, plato_id, cantidad, precio_unitario, nota) VALUES
(2, 4, 2, 38.00, 'Lomo tres cuartos'),
(2, 14, 1, 8.00, NULL);

INSERT INTO pedidos (id, mesa_id, mesero_id, cajero_id, estado, total, metodo_pago, fecha_creacion, fecha_pago) VALUES
(3, 1, 1, 3, 'pagado', 55.00, 'yape', DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 15 MINUTE));

INSERT INTO detalles_pedido (pedido_id, plato_id, cantidad, precio_unitario, nota) VALUES
(3, 5, 1, 28.00, NULL),
(3, 2, 1, 15.00, NULL),
(3, 9, 1, 12.00, NULL);
