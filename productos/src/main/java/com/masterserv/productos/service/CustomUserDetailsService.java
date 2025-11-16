package com.masterserv.productos.service;

import com.masterserv.productos.entity.Permiso; // Mentor: Importar Permiso
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
import java.util.HashSet; // Mentor: Importar HashSet

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
        
        // 1. Buscamos el usuario en nuestra BD (Tu código original, está perfecto)
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> 
                        new UsernameNotFoundException("Usuario no encontrado con email: " + email));

        // 2. Convertimos Roles Y PERMISOS a GrantedAuthority
        Collection<? extends GrantedAuthority> authorities = mapRolesAndPermissionsToAuthorities(usuario.getRoles());

        // 3. Creamos y retornamos el objeto UserDetails que Spring Security entiende
        return new User(
            usuario.getEmail(),
            usuario.getPasswordHash(),
            authorities // ¡Esta lista ahora contiene roles Y permisos!
        );
    }

    /**
     * Mentor: MÉTODO MODIFICADO
     * Este helper ahora extrae tanto los Roles como los Permisos anidados
     * y los convierte en una sola lista de GrantedAuthority.
     */
    private Collection<? extends GrantedAuthority> mapRolesAndPermissionsToAuthorities(Set<Rol> roles) {
        // Usamos un Set para evitar duplicados si un permiso se repite
        Set<GrantedAuthority> authorities = new HashSet<>();

        roles.forEach(rol -> {
            // 1. Agregamos el ROL (ej. "ROLE_ADMIN")
            authorities.add(new SimpleGrantedAuthority(rol.getNombreRol()));
            
            // 2. Agregamos todos los PERMISOS de ese rol (ej. "PRODUCTOS_MANAGE")
            rol.getPermisos().forEach(permiso -> {
                authorities.add(new SimpleGrantedAuthority(permiso.getNombrePermiso()));
            });
        });
        
        return authorities;
    }
}