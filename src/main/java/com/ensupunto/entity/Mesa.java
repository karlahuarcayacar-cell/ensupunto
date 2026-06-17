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
 * ENTIDAD ORM: MESA
 * 
 * CONCEPTOS ACADÉMICOS CLAVE:
 * 1. @Entity: Indica a JPA (Java Persistence API) e Hibernate (su proveedor de persistencia por defecto en Spring Boot)
 *    que esta clase es una Entidad que debe ser mapeada a una tabla relacional en la base de datos.
 * 
 * 2. @Table(name = "mesas"): Vincula la clase a la tabla física "mesas" en la base de datos.
 * 
 * 3. Lombok (@Data, @Builder, @NoArgsConstructor, @AllArgsConstructor):
 *    - @Data: Genera automáticamente métodos getters, setters, toString(), equals() y hashCode(). Redujo el código boilerplate.
 *    - @Builder: Implementa el patrón de diseño Builder, facilitando la creación fluida de objetos.
 *    - @NoArgsConstructor: Constructor vacío (obligatorio para la instanciación de Hibernate).
 *    - @AllArgsConstructor: Constructor con todos los parámetros (usado por el patrón Builder).
 * 
 * 4. @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}): Evita errores de serialización JSON.
 *    Hibernate crea proxies dinámicos para el soporte de carga diferida (Lazy Loading). Si se intenta convertir
 *    esta entidad a JSON, Jackson fallaría al leer las propiedades internas de esos proxies; esta anotación le
 *    indica a Jackson que las ignore.
 */
@Entity
@Table(name = "mesas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Mesa {
	
	    /**
	     * Clave Primaria Autoincremental.
	     * @GeneratedValue(strategy = GenerationType.IDENTITY): Delega la generación del ID a la base de datos (mediante AUTO_INCREMENT en MySQL).
	     */
	    @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Integer id;

	    /**
	     * Nombre de la mesa.
	     * Mapeado a VARCHAR(50), no nulo y único.
	     */
	    @Column(nullable = false, unique = true, length = 50)
	    private String nombre;

	    /**
	     * Estado de la mesa (Ej: 'libre', 'esperando_comida', 'cocina_preparacion', 'comiendo', 'cuenta_pedida').
	     * Mapeado a VARCHAR(30) no nulo. Valor por defecto "libre".
	     */
	    @Column(nullable = false, length = 30)
	    private String estado = "libre";

}
