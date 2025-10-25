package com.masterserv.productos.security;

import com.masterserv.productos.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.util.AntPathMatcher; // Necesario si usas shouldNotFilter

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    // ----- QUITAMOS ESTA LÓGICA REDUNDANTE -----
    // private final AntPathMatcher pathMatcher = new AntPathMatcher();
    // private static final List<String> PUBLIC_URLS = List.of(...);
    // @Override
    // protected boolean shouldNotFilter(HttpServletRequest request) { ... }
    // ---------------------------------------------
    // SecurityConfig es el único lugar donde deben definirse las rutas públicas.

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                      HttpServletResponse response,
                                      FilterChain filterChain)
            throws ServletException, IOException {

        // --- AGREGAR LOGS AQUÍ ---
        System.out.println(">>> JwtAuthenticationFilter ejecutándose para: " + request.getRequestURI()); // Log 1: ¿Se ejecuta?
        final String authHeader = request.getHeader("Authorization");
        System.out.println(">>> Header Authorization: " + authHeader); // Log 2: ¿Llega el header?
        // ------------------------

        // Si no hay token o no es Bearer, dejamos pasar (puede ser una ruta pública o fallará después)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println(">>> No hay token Bearer, continuando cadena..."); // Log 3
            filterChain.doFilter(request, response);
            return;
        }

        // Extraemos el token
        final String jwtToken = authHeader.substring(7);
        System.out.println(">>> Token JWT extraído: " + jwtToken); // Log 4: ¿Se extrae bien?

        try {
            // Obtenemos el email (username) del token
            String username = jwtTokenUtil.obtenerUsernameDelToken(jwtToken);
            System.out.println(">>> Username obtenido del token: " + username); // Log 5

            // Si tenemos username Y AÚN NO HAY AUTENTICACIÓN en el contexto de seguridad...
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // Cargamos los detalles del usuario desde la BD (llama a CustomUserDetailsService)
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                System.out.println(">>> UserDetails cargado: " + userDetails.getUsername() + ", Roles: " + userDetails.getAuthorities()); // Log 6

                // Validamos el token contra los UserDetails
                if (jwtTokenUtil.validarToken(jwtToken, userDetails)) {
                    
                    // Creamos el objeto de autenticación de Spring Security
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // ¡ESTABLECEMOS LA AUTENTICACIÓN EN EL CONTEXTO!
                    // Ahora Spring Security sabe que este usuario está autenticado para esta petición.
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    System.out.println(">>> Usuario autenticado y establecido en SecurityContextHolder!"); // Log 7
                } else {
                     System.out.println(">>> Token NO válido para UserDetails."); // Log 8
                }
            } else {
                 System.out.println(">>> Username es null o ya hay autenticación en el contexto."); // Log 9
            }

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            System.out.println("⚠️ Token expirado: " + e.getMessage());
            // Podríamos enviar una respuesta 401 específica aquí si quisiéramos
        } catch (io.jsonwebtoken.SignatureException e) {
            System.out.println("⚠️ Firma inválida del token: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("⚠️ Error procesando token JWT: " + e.getMessage());
            e.printStackTrace(); // Imprime el stack trace completo del error
        }

        // Continuamos la cadena de filtros (incluso si hubo error procesando el token)
        filterChain.doFilter(request, response);
    }
}