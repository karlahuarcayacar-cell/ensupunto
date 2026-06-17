package com.ensupunto;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * CONFIGURACIÓN DE DESPLIEGUE EN CONTENEDORES EXTERNOS (SERVLET INITIALIZER)
 * 
 * CONCEPTOS ACADÉMICOS CLAVE:
 * 1. SpringBootServletInitializer: Es una clase abstracta de Spring Boot que permite configurar
 *    el arranque de la aplicación cuando es desplegada como un archivo WAR (Web Application Archive)
 *    dentro de un servidor de aplicaciones externo (por ejemplo: Apache Tomcat, WildFly o GlassFish).
 * 
 * 2. Método configure: Sobrescribe el método configure para enlazar el ciclo de vida del contenedor
 *    externo con el de la aplicación Spring Boot. Indica al servidor web dónde encontrar la clase
 *    principal (EnsupuntoApplication) que inicializará todo el contexto y la inyección de dependencias.
 * 
 * NOTA: Esto no afecta la ejecución local con Tomcat embebido (mediante el plugin de Maven),
 * pero es crucial para entornos de producción tradicionales donde el servidor web corre de forma
 * independiente y gestiona múltiples aplicaciones en archivos .war.
 */
public class ServletInitializer extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		// Enlaza la inicialización de Spring Boot pasándole nuestra clase anotada con @SpringBootApplication.
		return application.sources(EnsupuntoApplication.class);
	}

}
