package com.ensupunto.entity;

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

@Entity
@Table(name = "platos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Plato {

	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Integer id;

	    @Column(nullable = false, unique = true, length = 100)
	    private String nombre;

	    @Column(nullable = false, length = 30)
	    private String categoria; 

	    @Column(nullable = false, precision = 10, scale = 2)
	    private java.math.BigDecimal precio;

	    @Column(columnDefinition = "TEXT")
	    private String descripcion;

	    @Column(nullable = false)
	    private Boolean activo = true;
}
