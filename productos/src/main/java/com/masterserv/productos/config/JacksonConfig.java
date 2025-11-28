package com.masterserv.productos.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature; // <-- IMPORTAR
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 1. Módulo para Hibernate (Lazy Loading)
        Hibernate6Module hibernate6Module = new Hibernate6Module();
        objectMapper.registerModule(hibernate6Module);

        // 2. Módulo para Fechas Java 8
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        objectMapper.registerModule(javaTimeModule);

        // --- Mentor: ESTA ES LA SOLUCIÓN DEFINITIVA ---
        // Forzamos a Jackson a escribir fechas como Strings (ISO-8601)
        // y NO como arrays de números (Timestamps).
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // --- Mentor: FIN ---

        return objectMapper;
    }
}