package com.ensupunto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CLASE PRINCIPAL DE LA APLICACIÓN (ENTRY POINT)
 * 
 * En el ecosistema de Spring Boot, esta clase actúa como el punto de partida.
 * 
 * CONCEPTOS ACADÉMICOS CLAVE:
 * 1. @SpringBootApplication: Es una anotación de conveniencia que combina tres anotaciones esenciales:
 *    - @Configuration: Indica que la clase contiene definiciones de beans de Spring (métodos con @Bean)
 *      y permite configurar el contexto de la aplicación.
 *    - @EnableAutoConfiguration: Habilita el mecanismo de autoconfiguración de Spring Boot, el cual
 *      intenta configurar automáticamente los beans necesarios según las dependencias del classpath (pom.xml).
 *      Por ejemplo, si detecta mysql-connector-j, autoconfigura el DataSource para la base de datos.
 *    - @ComponentScan: Activa el escaneo automático de componentes a partir del paquete actual ("com.ensupunto").
 *      Spring buscará clases anotadas con @Component, @Controller, @Service, @Repository, entre otras,
 *      y las registrará en el contenedor de inversión de control (IoC).
 */
@SpringBootApplication
public class EnsupuntoApplication {

	/**
	 * Método estándar de arranque en Java (entry point).
	 * Al ejecutar la aplicación, este método inicia la JVM y arranca el contenedor de Spring.
	 */
	public static void main(String[] args) {
		// Establecemos la localización por defecto a español de Perú para formatear correctamente
		// monedas (S/.) y fechas de acuerdo a las convenciones nacionales durante los reportes.
		java.util.Locale.setDefault(new java.util.Locale("es", "PE"));
		
		// Llama a SpringApplication.run para levantar el servidor Tomcat embebido,
		// inicializar el contexto de Spring (ApplicationContext) y activar la inyección de dependencias.
		SpringApplication.run(EnsupuntoApplication.class, args);
	}

}
