package com.masterserv.productos.service;

import com.masterserv.productos.entity.Rol;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Este método es llamado por Spring Security durante el proceso de autenticación.
     * Carga los detalles del usuario desde la base de datos usando el email (que tratamos como 'username').
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        
        // 1. Buscamos el usuario en nuestra BD usando el método que creamos
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> 
                        new UsernameNotFoundException("Usuario no encontrado con email: " + email));

        // 2. Convertimos nuestros Roles (de la entidad) a GrantedAuthority (de Spring Security)
        Collection<? extends GrantedAuthority> authorities = mapRolesToAuthorities(usuario.getRoles());

        // 3. Creamos y retornamos el objeto UserDetails que Spring Security entiende
        return new User(
            usuario.getEmail(),
            usuario.getPasswordHash(),
            authorities
        );
        // NOTA: Spring Security se encargará de verificar el estado (activo, bloqueado) 
        // y de comparar el password hash.
    }

    /**
     * Método helper para convertir nuestro Set<Rol> en una Collection<GrantedAuthority>.
     * Spring Security necesita este formato para manejar la autorización.
     */
    private Collection<? extends GrantedAuthority> mapRolesToAuthorities(Set<Rol> roles) {
        return roles.stream()
                .map(rol -> new SimpleGrantedAuthority(rol.getNombreRol()))
                .collect(Collectors.toList());
    }
}