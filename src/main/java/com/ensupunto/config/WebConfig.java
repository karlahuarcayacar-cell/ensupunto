package com.ensupunto.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CONFIGURACIÓN DE MIDDLEWARES Y COMPORTAMIENTO WEB (WEB MVC CONFIGURATION)
 * 
 * CONCEPTOS ACADÉMICOS CLAVE:
 * 1. @Configuration: Indica que esta clase define configuraciones de infraestructura a nivel de Spring Boot.
 *    Es escaneada por el contenedor y sus métodos anotados con @Bean registran objetos para su inyección.
 * 
 * 2. WebMvcConfigurer: Es una interfaz de Spring MVC que proporciona métodos callback para personalizar
 *    la configuración de Spring MVC basada en Java (registros de interceptores, mapeos de recursos estáticos,
 *    configuraciones de CORS, formateadores, etc.) sin sobrescribir toda la configuración predeterminada.
 * 
 * 3. Inyección de dependencias (DI): Inyectamos 'AuthInterceptor' para poder registrarlo formalmente
 *    dentro de la cadena de ejecución de peticiones HTTP.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    /**
     * Registra interceptores personalizados creados para la aplicación web.
     * 
     * @param registry Ayuda a registrar y configurar interceptores de peticiones HTTP.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Añadimos nuestro AuthInterceptor al registro global.
        // .addPathPatterns("/**") indica que el interceptor evaluará absolutamente todas las rutas.
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**");
    }

    /**
     * Define un Bean para resolver la localización geográfica (Locale) regional de la sesión.
     * Esto asegura que las fechas, números y monedas se formateen según las normas peruanas en las vistas de Thymeleaf.
     * 
     * @return LocaleResolver configurado para "es-PE" (Español de Perú).
     */
    @org.springframework.context.annotation.Bean
    public org.springframework.web.servlet.LocaleResolver localeResolver() {
        org.springframework.web.servlet.i18n.SessionLocaleResolver slr = new org.springframework.web.servlet.i18n.SessionLocaleResolver();
        slr.setDefaultLocale(new java.util.Locale("es", "PE"));
        return slr;
    }
}
