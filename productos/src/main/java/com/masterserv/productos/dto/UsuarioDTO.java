package com.masterserv.productos.dto;

import com.masterserv.productos.enums.EstadoUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode; // Importar

import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true) // Necesario por heredar de AuditableDTO
public class UsuarioDTO extends AuditableDTO { // <-- HEREDA
    
    private Long id; 

    @NotEmpty(message = "El nombre es obligatorio")
    @Size(min = 2, max = 50)
    private String nombre;

    @NotEmpty(message = "El apellido es obligatorio")
    @Size(min = 2, max = 50)
    private String apellido;

    @NotEmpty(message = "El email es obligatorio")
    @Email
    @Size(max = 100)
    private String email;

    private String passwordHash; // Solo para ENTRADA (Crear/Actualizar)

    @Size(max = 30)
    private String documento;

    @Size(max = 20)
    private String telefono;

    private EstadoUsuario estado;
    
    private Long tipoDocumentoId;
    
    private Set<RolDTO> roles; 
}