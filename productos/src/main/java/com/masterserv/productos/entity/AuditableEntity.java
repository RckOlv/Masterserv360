package com.masterserv.productos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass 
@EntityListeners(AuditingEntityListener.class) 
public abstract class AuditableEntity {

    @CreatedDate 
    @Column(name = "fecha_creacion", nullable = false, updatable = false) // Añadí nullable = false
    protected LocalDateTime fechaCreacion;

    @LastModifiedDate 
    @Column(name = "fecha_modificacion", nullable = false) // Añadí nullable = false
    protected LocalDateTime fechaModificacion;
}