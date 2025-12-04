package com.masterserv.productos.entity;

import com.fasterxml.jackson.databind.ObjectMapper; // Importante
import com.masterserv.productos.config.BeanUtil;   // Importante
import com.masterserv.productos.listener.AuditoriaListener;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass 
@EntityListeners({AuditingEntityListener.class, AuditoriaListener.class}) 
public abstract class AuditableEntity {

    @CreatedDate 
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    protected LocalDateTime fechaCreacion;

    @LastModifiedDate 
    @Column(name = "fecha_modificacion", nullable = false)
    protected LocalDateTime fechaModificacion;

    @Transient
    private String estadoAnterior; // Aquí guardaremos el JSON viejo

    @PostLoad
    public void cargarEstadoAnterior() {
        try {
            // Usamos Jackson para convertir el objeto a JSON String (Texto legible)
            ObjectMapper mapper = BeanUtil.getBean(ObjectMapper.class);
            this.estadoAnterior = mapper.writeValueAsString(this);
        } catch (Exception e) {
            this.estadoAnterior = "Error al serializar: " + e.getMessage();
        }
    }
    
    public String getEstadoAnterior() {
        return estadoAnterior;
    }
}