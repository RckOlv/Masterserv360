package com.masterserv.productos;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.crypto.password.PasswordEncoder;


@SpringBootApplication
@EnableJpaAuditing
public class ProductosApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductosApplication.class, args);
    }

    /**
     * Bean que se ejecuta al inicio para imprimir el hash de una contraseña de prueba.
     */
    /**@Bean
    public CommandLineRunner run(PasswordEncoder passwordEncoder) {
        return args -> {
            String testPassword = "password"; // Usa la contraseña que quieras
            String hashedPassword = passwordEncoder.encode(testPassword);
            
            System.out.println("=========================================================");
            System.out.println("=== 🔐 HASH GENERADO PARA LA CONTRASEÑA: " + testPassword + " ===");
            System.out.println("=== COPIA ESTE HASH Y ÚSALO EN TU COMANDO SQL ABAJO: ====");
            System.out.println("HASH: " + hashedPassword);
            System.out.println("=========================================================");
        };
    }
        */
}