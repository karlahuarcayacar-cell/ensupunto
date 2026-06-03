-- ====================================================================
-- BASE DE DATOS: En su punto SAC (ensupunto_db)
-- Autor: Antigravity AI Pair Programmer
-- Compatible con: MySQL 8.x, Spring Boot Web, Spring Data JPA, Lombok
-- ====================================================================

-- 1. CREACIÓN DE LA BASE DE DATOS
CREATE DATABASE IF NOT EXISTS ensupunto_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ensupunto_db;

-- 2. ELIMINACIÓN DE TABLAS (En orden de dependencia para evitar conflictos de FK)
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS modificaciones_pedido;
DROP TABLE IF EXISTS pagos_fraccionados;
DROP TABLE IF EXISTS detalles_pedido;
DROP TABLE IF EXISTS pedidos;
DROP TABLE IF EXISTS mesas;
DROP TABLE IF EXISTS platos;
DROP TABLE IF EXISTS usuarios;
SET FOREIGN_KEY_CHECKS = 1;

-- ====================================================================
-- 3. CREACIÓN DE TABLAS
-- ====================================================================

-- Tabla: usuarios
-- Rol de JPA: Entidad `Usuario` (@Entity, @Table(name = "usuarios"))
-- Lombok: @Data, @Builder, @NoArgsConstructor, @AllArgsConstructor
CREATE TABLE usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    rol VARCHAR(20) NOT NULL, -- Valores: 'mesero', 'chef', 'cajero', 'admin'
    nombre_usuario VARCHAR(50) NOT NULL UNIQUE, -- Para Login (CU-01)
    contrasena VARCHAR(255) NOT NULL, -- En producción se recomienda guardar con BCrypt
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Tabla: platos
-- Rol de JPA: Entidad `Plato` (@Entity, @Table(name = "platos"))
CREATE TABLE platos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    categoria VARCHAR(30) NOT NULL, -- Valores: 'entradas', 'segundos', 'bebidas', 'postres'
    precio DECIMAL(10, 2) NOT NULL,
    descripcion TEXT,
    activo BOOLEAN NOT NULL DEFAULT TRUE -- Para baja lógica (CU-05)
) ENGINE=InnoDB;

-- Tabla: mesas
-- Rol de JPA: Entidad `Mesa` (@Entity, @Table(name = "mesas"))
CREATE TABLE mesas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE, -- Ej: 'Mesa 1', 'Mesa 2'
    estado VARCHAR(30) NOT NULL DEFAULT 'libre' -- Valores: 'libre', 'esperando_comida', 'cocina_preparacion', 'comiendo', 'cuenta_pedida'
) ENGINE=InnoDB;

-- Tabla: pedidos
-- Rol de JPA: Entidad `Pedido` (@Entity, @Table(name = "pedidos"))
-- Relaciones JPA: 
--  - @ManyToOne con Mesa
--  - @ManyToOne con Usuario (Mesero)
--  - @ManyToOne con Usuario (Cajero) - Opcional/Nuleable
--  - @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL) con Detalles
--  - @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL) con PagosFraccionados
CREATE TABLE pedidos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    mesa_id INT NOT NULL,
    mesero_id INT NOT NULL,
    cajero_id INT NULL, -- Registrado al cobrar la cuenta (CU-04 / CU-09)
    estado VARCHAR(30) NOT NULL DEFAULT 'pendiente', -- Valores: 'pendiente', 'cocina_pendiente', 'cocina_preparacion', 'cocina_listo', 'servido', 'cuenta_pedida', 'pagado', 'dividido'
    total DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    metodo_pago VARCHAR(30) NULL, -- Valores: 'efectivo', 'tarjeta', 'yape', 'dividido'
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_pago TIMESTAMP NULL, -- Se llena en el CU-04 / CU-09 al finalizar la transacción
    CONSTRAINT fk_pedidos_mesa FOREIGN KEY (mesa_id) REFERENCES mesas(id),
    CONSTRAINT fk_pedidos_mesero FOREIGN KEY (mesero_id) REFERENCES usuarios(id),
    CONSTRAINT fk_pedidos_cajero FOREIGN KEY (cajero_id) REFERENCES usuarios(id)
) ENGINE=InnoDB;

-- Tabla: detalles_pedido
-- Rol de JPA: Entidad `DetallePedido` (@Entity, @Table(name = "detalles_pedido"))
-- Relaciones JPA:
--  - @ManyToOne con Pedido (@JoinColumn(name = "pedido_id"))
--  - @ManyToOne con Plato (@JoinColumn(name = "plato_id"))
CREATE TABLE detalles_pedido (
    id INT AUTO_INCREMENT PRIMARY KEY,
    pedido_id INT NOT NULL,
    plato_id INT NOT NULL,
    cantidad INT NOT NULL DEFAULT 1,
    precio_unitario DECIMAL(10, 2) NOT NULL, -- Histórico de precio para evitar distorsión si el plato cambia de precio
    nota VARCHAR(255) NULL, -- Ej: 'sin cebolla' (CU-02)
    CONSTRAINT fk_detalles_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE,
    CONSTRAINT fk_detalles_plato FOREIGN KEY (plato_id) REFERENCES platos(id)
) ENGINE=InnoDB;

-- Tabla: pagos_fraccionados
-- Rol de JPA: Entidad `PagoFraccionado` (@Entity, @Table(name = "pagos_fraccionados"))
-- Relaciones JPA:
--  - @ManyToOne con Pedido (@JoinColumn(name = "pedido_id"))
CREATE TABLE pagos_fraccionados (
    id INT AUTO_INCREMENT PRIMARY KEY,
    pedido_id INT NOT NULL,
    numero_cliente INT NOT NULL, -- Ej: Cliente 1, Cliente 2...
    monto DECIMAL(10, 2) NOT NULL,
    metodo_pago VARCHAR(30) NULL, -- 'efectivo', 'tarjeta', 'yape'
    numero_boleta VARCHAR(30) NULL, -- Ej: 'B001-XXXXXX'
    pagado BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_pago TIMESTAMP NULL,
    CONSTRAINT fk_pagos_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Tabla: modificaciones_pedido
-- Rol de JPA: Entidad `ModificacionPedido` (@Entity, @Table(name = "modificaciones_pedido"))
-- Registra autorizaciones de anulación de platos que ya están en cocina (CU-08)
-- Relaciones JPA:
--  - @ManyToOne con Pedido (@JoinColumn(name = "pedido_id"))
--  - @ManyToOne con Usuario (Administrador) (@JoinColumn(name = "administrador_id"))
CREATE TABLE modificaciones_pedido (
    id INT AUTO_INCREMENT PRIMARY KEY,
    pedido_id INT NOT NULL,
    administrador_id INT NOT NULL, -- Quien autorizó la anulación/cambio
    detalle VARCHAR(255) NOT NULL, -- Detalle del cambio (ej: "Se retiró 1 Lomo Saltado")
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mod_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id) ON DELETE CASCADE,
    CONSTRAINT fk_mod_admin FOREIGN KEY (administrador_id) REFERENCES usuarios(id)
) ENGINE=InnoDB;

-- ====================================================================
-- 4. ÍNDICES DE RENDIMIENTO (Optimización para Consultas Frecuentes)
-- ====================================================================
CREATE INDEX idx_pedidos_estado ON pedidos(estado);
CREATE INDEX idx_pedidos_fecha ON pedidos(fecha_creacion);
CREATE INDEX idx_platos_categoria ON platos(categoria);
CREATE INDEX idx_usuarios_rol ON usuarios(rol);

-- ====================================================================
-- 5. INSERCIÓN DE DATOS INICIALES (MOCK DATA)
-- ====================================================================

-- 5.1 Usuarios por defecto (Contraseña: '123' para fines de simulación)
INSERT INTO usuarios (id, nombre, rol, nombre_usuario, contrasena, activo) VALUES
(1, 'Juan Pérez', 'mesero', 'mesero', '123', TRUE),
(2, 'Carlos Ruiz', 'chef', 'chef', '123', TRUE),
(3, 'María Gómez', 'cajero', 'cajero', '123', TRUE),
(4, 'Ana Martínez', 'admin', 'admin', '123', TRUE);

-- 5.2 Platos iniciales del menú (Categorizados)
INSERT INTO platos (id, nombre, categoria, precio, descripcion, activo) VALUES
-- Entradas
(1, 'Ceviche Carretillero', 'entradas', 25.00, 'Pescado fresco marinado en limón con cebolla, camote y choclo, coronado con chicharrón de pota.', TRUE),
(2, 'Papa a la Huancaína', 'entradas', 15.00, 'Papas sancochadas bañadas en salsa cremosa de ají amarillo, queso y leche.', TRUE),
(3, 'Causa Rellena de Pollo', 'entradas', 18.00, 'Masa de papa amarilla sazonada con ají amarillo y limón, rellena de pollo, mayonesa y palta.', TRUE),
-- Segundos
(4, 'Lomo Saltado', 'segundos', 38.00, 'Trozos de lomo de res salteados al wok con cebolla, tomate, ají amarillo, servido con papas fritas y arroz.', TRUE),
(5, 'Ají de Gallina', 'segundos', 28.00, 'Pechuga de pollo deshilachada en crema de ají amarillo, leche y nueces, acompañado de arroz y papa.', TRUE),
(6, 'Arroz con Mariscos', 'segundos', 35.00, 'Arroz sazonado con aderezo norteño y mariscos de estación (langostinos, calamar y conchas).', TRUE),
(7, 'Cabrito a la Norteña', 'segundos', 42.00, 'Tierno cabrito macerado en chicha de jora y culantro, servido con frejoles, yuca y arroz.', TRUE),
-- Bebidas
(8, 'Chicha Morada 1L', 'bebidas', 15.00, 'Bebida tradicional de maíz morado hervido con piña, manzana, canela y clavo de olor.', TRUE),
(9, 'Limonada Frozen', 'bebidas', 12.00, 'Limonada refrescante batida con hielo picado.', TRUE),
(10, 'Gaseosa Personal', 'bebidas', 6.00, 'Coca Cola o Inka Cola personal (500ml).', TRUE),
(11, 'Cerveza Pilsen', 'bebidas', 10.00, 'Cerveza nacional en botella personal.', TRUE),
-- Postres
(12, 'Suspiro a la Limeña', 'postres', 12.00, 'Crema de manjarblanco y yemas, coronado con merengue al oporto.', TRUE),
(13, 'Torta Tres Leches', 'postres', 10.00, 'Bizcochuelo bañado en tres tipos de leche, decorado con chantilly y canela.', TRUE),
(14, 'Crema Volteada', 'postres', 8.00, 'Clásico flan de leche condensada con caramelo líquido.', TRUE);

-- 5.3 Mesas físicas del restaurante
INSERT INTO mesas (id, nombre, estado) VALUES
(1, 'Mesa 1', 'libre'),
(2, 'Mesa 2', 'libre'),
(3, 'Mesa 3', 'libre'),
(4, 'Mesa 4', 'libre'),
(5, 'Mesa 5', 'libre'),
(6, 'Mesa 6', 'libre'),
(7, 'Mesa 7', 'libre'),
(8, 'Mesa 8', 'libre');

-- 5.4 Historial de Ventas iniciales (Mock de Pedidos Cobrados para Reportes CU-07)
-- Pedido 1: Mesa 2 (Pagado por Efectivo)
INSERT INTO pedidos (id, mesa_id, mesero_id, cajero_id, estado, total, metodo_pago, fecha_creacion, fecha_pago) VALUES
(1, 2, 1, 3, 'pagado', 78.00, 'efectivo', DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 105 MINUTE));

INSERT INTO detalles_pedido (pedido_id, plato_id, cantidad, precio_unitario, nota) VALUES
(1, 1, 2, 25.00, 'Uno sin ají'), -- 2 Ceviches (50.00)
(1, 8, 1, 15.00, NULL),       -- 1 Chicha 1L (15.00)
(1, 12, 1, 12.00, NULL);      -- 1 Suspiro (12.00)

-- Pedido 2: Mesa 5 (Pagado por Tarjeta)
INSERT INTO pedidos (id, mesa_id, mesero_id, cajero_id, estado, total, metodo_pago, fecha_creacion, fecha_pago) VALUES
(2, 5, 1, 3, 'pagado', 84.00, 'tarjeta', DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 40 MINUTE));

INSERT INTO detalles_pedido (pedido_id, plato_id, cantidad, precio_unitario, nota) VALUES
(2, 4, 2, 38.00, 'Lomo tres cuartos'), -- 2 Lomo Saltado (76.00)
(2, 14, 1, 8.00, NULL);                -- 1 Crema Volteada (8.00)

-- Pedido 3: Mesa 1 (Pagado con Yape)
INSERT INTO pedidos (id, mesa_id, mesero_id, cajero_id, estado, total, metodo_pago, fecha_creacion, fecha_pago) VALUES
(3, 1, 1, 3, 'pagado', 55.00, 'yape', DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 15 MINUTE));

INSERT INTO detalles_pedido (pedido_id, plato_id, cantidad, precio_unitario, nota) VALUES
(3, 5, 1, 28.00, NULL),  -- 1 Ají de gallina (28.00)
(3, 2, 1, 15.00, NULL),  -- 1 Papa a la Huancaína (15.00)
(3, 9, 1, 12.00, NULL);  -- 1 Limonada Frozen (12.00)

-- ====================================================================
-- FIN DEL SCRIPT.
-- ====================================================================
