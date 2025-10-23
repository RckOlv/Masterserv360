package com.masterserv.productos.config;

import com.masterserv.productos.security.JwtAuthenticationFilter;
import com.masterserv.productos.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
@EnableMethodSecurity(prePostEnabled = true) // Habilita @PreAuthorize en los controladores
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private CustomUserDetailsService customUserDetailsService; // Lo usaremos indirectamente

    /**
     * Define la cadena de filtros de seguridad.
     * Aquí es donde "cerramos" la API.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Deshabilitamos CSRF (no necesario para APIs REST stateless)
            .csrf(csrf -> csrf.disable())
            
            // 2. Configuramos CORS (usando el Bean de abajo)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 3. Definimos las reglas de autorización
            .authorizeHttpRequests(authz -> authz
                // Nuestros endpoints públicos (Login y Registro)
                .requestMatchers("/api/auth/**").permitAll() 
                .requestMatchers("/api/chatbot/**").permitAll() // Permite el webhook de Twilio
                // Todos los demás endpoints requieren autenticación
                .anyRequest().authenticated() 
            )
            
            // 4. Establecemos la política de sesión como STATELESS (sin estado)
            // Spring Security no creará ni usará sesiones HTTP.
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // 5. Agregamos nuestro filtro JWT ANTES del filtro de login estándar
            // Esto asegura que validamos el token en cada petición.
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Bean para el PasswordEncoder (BCrypt).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Bean para el AuthenticationManager.
     * Spring lo usará para orquestar el login, llamando a nuestro CustomUserDetailsService.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Configuración Global de CORS.
     * Reemplaza todos los @CrossOrigin(origins = "http://localhost:4200")
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Permitimos el origen de nuestro frontend de Angular
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        // Permitimos los métodos HTTP estándar
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        // Permitimos todas las cabeceras (incluyendo "Authorization")
        configuration.setAllowedHeaders(List.of("*"));
        // Permitimos que el frontend reciba credenciales (como el token)
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Aplicamos esta configuración a TODAS las rutas de nuestra API
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}