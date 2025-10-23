package com.masterserv.productos.entity;

// Importamos el Enum que acabamos de crear
import com.masterserv.productos.enums.EstadoUsuario;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

@Entity
@Table(name = "usuarios",
    uniqueConstraints = {
        // Clave única compuesta que discutimos
        @UniqueConstraint(columnNames = {"tipo_documento_id", "documento"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

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
    private String passwordHash; // Almacenará el hash de bcrypt

    @Column(length = 30) // Anulable
    private String documento;

    @Column(length = 20) // Anulable
    private String telefono;

    @Enumerated(EnumType.STRING) // ¡La clave! Guarda "ACTIVO", "INACTIVO", etc.
    @Column(nullable = false, length = 25)
    private EstadoUsuario estado;

    // --- Relaciones ---

    @ManyToOne(fetch = FetchType.LAZY) // Un usuario tiene UN tipo de documento
    @JoinColumn(name = "tipo_documento_id") // FK, anulable por defecto
    private TipoDocumento tipoDocumento;

    @ManyToMany(fetch = FetchType.EAGER) // Un usuario tiene MUCHOS roles
    @JoinTable(
        name = "usuarios_roles", // Nombre de la tabla de unión
        joinColumns = @JoinColumn(name = "usuario_id"), // FK a esta entidad
        inverseJoinColumns = @JoinColumn(name = "rol_id") // FK a la otra entidad
    )
    private Set<Rol> roles;
    
    // NOTA: No necesitamos las relaciones inversas (@OneToMany)
    // a Ventas, Pedidos, etc. aquí. Las mantendremos simples
    // por ahora para acelerar el desarrollo.
}