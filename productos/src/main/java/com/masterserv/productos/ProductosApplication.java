package com.masterserv.productos;

import jakarta.annotation.PostConstruct; // <--- Importante para configurar al inicio
import java.util.TimeZone; // <--- Importante para la zona horaria

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class ProductosApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductosApplication.class, args);
    }

    // --- ESTE MÉTODO ARREGLA LA HORA ---
    @PostConstruct
    public void init() {
        // Configuramos la zona horaria a Argentina (Buenos Aires GMT-3)
        // Así las ventas se guardarán con tu hora real, no la de Londres/UTC.
        TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"));
        System.out.println("✅ Zona horaria configurada a: " + TimeZone.getDefault().getID());
    }
}