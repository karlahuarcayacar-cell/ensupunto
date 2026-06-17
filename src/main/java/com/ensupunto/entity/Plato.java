package com.ensupunto.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ENTIDAD ORM: PLATO
 * 
 * CONCEPTOS ACADÉMICOS CLAVE:
 * 1. @Entity: Declara que esta clase es un modelo JPA persistente.
 * 2. @Table(name = "platos"): Representa la tabla física de platos en el menú.
 * 3. @Data, @Builder, etc. de Lombok para eliminar código repetitivo de encapsulamiento.
 * 4. Control de decimales en base de datos: En Java usamos 'BigDecimal' y en MySQL 'DECIMAL(10,2)'
 *    para evitar errores de redondeo de punto flotante en importes de dinero.
 * 5. Baja lógica: El campo 'activo' permite deshabilitar platos sin eliminarlos físicamente,
 *    evitando romper la integridad referencial de los pedidos históricos.
 */
@Entity
@Table(name = "platos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Plato {

	    @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Integer id;

	    // Nombre del plato, único e imprescindible.
	    @Column(nullable = false, unique = true, length = 100)
	    private String nombre;

	    // Categoría del plato (Ej: 'entradas', 'segundos', 'bebidas', 'postres').
	    @Column(nullable = false, length = 30)
	    private String categoria; 

	    // Precio unitario. Se define precisión 10 y escala 2 para importes financieros (S/. 99,999,999.99).
	    @Column(nullable = false, precision = 10, scale = 2)
	    private java.math.BigDecimal precio;

	    // Descripción textual larga. columnDefinition="TEXT" permite guardar textos extensos (recetas, ingredientes).
	    @Column(columnDefinition = "TEXT")
	    private String descripcion;

	    // Bandera de estado activo para implementar Baja Lógica (CRUD de Admin).
	    @Column(nullable = false)
	    private Boolean activo = true;
}
