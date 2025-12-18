package com.masterserv.productos.entity;

import com.masterserv.productos.enums.EstadoUsuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

@Entity
@Table(name = "usuarios",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tipo_documento_id", "documento"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario extends AuditableEntity {

    // ... (Tus campos id, nombre, apellido, email, passwordHash, documento, telefono existentes) ...
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(nullable = false, length = 50)
    private String apellido;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(length = 30)
    private String documento;

    @Column(length = 20)
    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private EstadoUsuario estado;

    // --- NUEVO CAMPO ---
    // Indica si el usuario debe cambiar su contraseña al próximo login
    @Column(name = "debe_cambiar_password")
    private boolean debeCambiarPassword = false; 
    // -------------------

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_documento_id")
    private TipoDocumento tipoDocumento;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "usuarios_roles",
        joinColumns = @JoinColumn(name = "usuario_id"),
        inverseJoinColumns = @JoinColumn(name = "rol_id")
    )
    private Set<Rol> roles;
    
    // Lombok genera automáticamente: isDebeCambiarPassword() y setDebeCambiarPassword()
}