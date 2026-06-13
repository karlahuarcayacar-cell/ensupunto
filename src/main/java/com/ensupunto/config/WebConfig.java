package com.ensupunto.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**"); // Intercepts all
    }

    @org.springframework.context.annotation.Bean
    public org.springframework.web.servlet.LocaleResolver localeResolver() {
        org.springframework.web.servlet.i18n.SessionLocaleResolver slr = new org.springframework.web.servlet.i18n.SessionLocaleResolver();
        slr.setDefaultLocale(new java.util.Locale("es", "PE"));
        return slr;
    }
}
