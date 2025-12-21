package com.masterserv.productos.config;

import com.masterserv.productos.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; 
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. CONFIGURACIÓN CSRF (Vital para Webhooks externos como Twilio)
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/bot/**") // Ignorar CSRF específicamente para el bot
                .disable() // Deshabilitar globalmente (para APIs REST es común)
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) 

            // 2. AUTORIZACIÓN DE RUTAS
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/").permitAll()
                
                // --- CAMBIO CLAVE: Usar comodín /** para asegurar que entra ---
                .requestMatchers("/bot/**").permitAll() 
                // -------------------------------------------------------------

                .requestMatchers("/auth/**", "/error").permitAll()
                .requestMatchers("/auth/forgot-password", "/auth/reset-password").permitAll()
                
                // Rutas de Consulta Pública
                .requestMatchers(HttpMethod.GET, "/productos/**").permitAll() 
                .requestMatchers(HttpMethod.POST, "/productos/filtrar").permitAll() 
                .requestMatchers(HttpMethod.GET, "/categorias/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/tipos-documento/**").permitAll()
                
                // Rutas Públicas adicionales
                .requestMatchers("/public/**").permitAll() 

                // Todo lo demás requiere autenticación (JWT)
                .anyRequest().authenticated() 
            )

            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Orígenes permitidos (Local y Producción)
        configuration.setAllowedOrigins(List.of(
            "http://localhost:4200", 
            "http://localhost:4300",
            "https://masterserv360.vercel.app"
        )); 
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); 

        return source;
    }
}