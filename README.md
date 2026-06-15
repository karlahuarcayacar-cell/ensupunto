# En su punto S.A.C. - Sistema de Gestión de Restaurante 🍽️

**En su punto** es una aplicación web empresarial de nivel académico diseñada para optimizar los procesos de atención, preparación, cobro y análisis gerencial dentro de un restaurante. 

El sistema implementa una arquitectura desacoplada por roles que permite la interacción en tiempo real entre el personal de salón, cocina y caja, ofreciendo una experiencia moderna tipo **SPA (Single Page Application)** mediante el uso de **Spring Boot** y **HTMX** (evitando recargas totales del navegador).

---

## 🚀 Características y Módulos del Sistema

### 1. Panel de Administración y Control Gerencial (Admin)
Módulo centralizado para la configuración del negocio y auditoría:
* **Mantenimiento completo (3 CRUDs):**
  * **Mantenimiento de Platos:** Creación, edición, precios y baja lógica (activo/inactivo) de los platos y bebidas de la carta.
  * **Mantenimiento de Personal:** Registro y edición de cuentas de empleados asignando roles del sistema (Mesero, Chef, Cajero, Administrador).
  * **Mantenimiento de Mesas:** Gestión física del salón (alta de mesas, asignación y estado inicial).
* **Filtros de Reporte de Ventas Consolidados:** 
  * Consulta de ingresos en tiempo real filtrados por: *Ventas del Día*, *Histórico Completo* o *Rango Personalizado* con controles de paginación reactiva.
  * Generación y visualización dinámica de reportes **PDF (JasperReports)** integrados mediante iframe (vista previa, impresión directa y descarga).
* **Métricas Clave (KPIs):** Resumen diario de ingresos, transacciones completadas y promedio de consumo por ticket.

### 2. Módulo del Mesero (Atención en Salón)
* **Mapa de Mesas Interactivo:** Representación visual del salón que cambia de color y estado físico en tiempo real (*Libre, En Cola, Preparando, Comiendo, Cuenta Pedida*).
* **Toma de Pedidos:** Carrito de compras interactivo con capacidad de agregar notas de preparación por plato (ej. *"sin cebolla"*, *"término medio"*).
* **Control de Seguridad de Cambios:** En caso de que el mesero intente anular o modificar un plato que ya está en proceso de cocción, el sistema solicita credenciales de autorización de un Administrador, generando una bitácora de auditoría.

### 3. Módulo de Cocina (Chef Monitor)
* **Cola de Preparación:** Monitor que organiza los platos según prioridad de llegada.
* **Control de Estados:** Permite al Chef actualizar el estado del pedido a *"En Preparación"* y *"Listo para Servir"*, alertando automáticamente al mesero.

### 4. Módulo de Caja y Facturación (Cajero)
* **Cobro Regular:** Emisión de comprobantes de pago oficiales (*Boleta* o *Factura*) con cálculo automático de vuelto y método de pago (Efectivo, Tarjeta, Yape).
* **Cuentas Divididas:** Soporte para cobro fraccionado, dividiendo el importe total equitativamente entre $N$ clientes, permitiendo a cada uno pagar por separado con su respectivo método y boleta.
* **Liberación de Mesa:** Notificación y liberación automática de la mesa en el mapa tras saldar la totalidad de la cuenta.

---

## 🛠️ Stack Tecnológico

* **Lenguaje:** Java 17
* **Framework:** Spring Boot 3.x / 4.x (Web MVC, Data JPA)
* **Plantillas:** Thymeleaf (con Layout Dialect)
* **Interactividad:** HTMX 1.9 (AJAX reactivo y modular)
* **Base de Datos:** MySQL 8.x
* **Reportes:** JasperReports 6.21.0 (PDF Engine)
* **Estilizado:** Vanilla CSS (CSS3 Moderno, Variables CSS, Glassmorphism, Responsive Grid)
* **Librerías Auxiliares:** Lombok (Productividad de código boilerplate)

---

## 💾 Configuración de la Base de Datos

1. Abra su cliente MySQL (Workbench, DBeaver, CLI) e importe el script SQL de creación de estructura y mock data:
   * Archivo: `db_restaurante .sql` (ubicado en la raíz del proyecto).
2. Este script creará la base de datos `ensupunto_db`, las tablas necesarias (`usuarios`, `platos`, `mesas`, `pedidos`, etc.) e insertará datos iniciales de prueba.
3. Las credenciales por defecto configuradas en el archivo [application.properties](file:///C:/Users/antwn/Desktop/WS%20Proyecto%20LPII/ensupunto/src/main/resources/application.properties) son:
   * **URL:** `jdbc:mysql://localhost:3306/ensupunto_db`
   * **Usuario:** `root`
   * **Contraseña:** `mysql` (Modifique este campo si su contraseña de MySQL es diferente).

---

## 🏃 Cómo Ejecutar el Proyecto

1. Asegúrese de tener instalado el JDK 17 y de que el puerto `8080` de su sistema esté libre.
2. Abra una terminal en la raíz del proyecto y ejecute el siguiente comando para compilar y levantar la aplicación:
   ```bash
   # En Windows:
   .\mvnw spring-boot:run

   # En Linux / macOS:
   ./mvnw spring-boot:run
   ```
3. Abra su navegador web e ingrese a la siguiente dirección:
   * 👉 **`http://localhost:8080/`** (Redirige automáticamente a la pantalla de Login).

### 🔑 Cuentas de Acceso de Prueba (Mock Data)
| Rol | Usuario | Contraseña |
|---|---|---|
| **Administrador** | `admin` | `123` |
| **Mesero** | `mesero` | `123` |
| **Chef** | `chef` | `123` |
| **Cajero** | `cajero` | `123` |
