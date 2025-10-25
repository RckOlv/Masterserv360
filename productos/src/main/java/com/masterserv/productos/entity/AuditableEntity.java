package com.masterserv.productos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@MappedSuperclass // 1. Indica que es una plantilla, no una tabla
@EntityListeners(AuditingEntityListener.class) // 2. Activa la auditoría
public abstract class AuditableEntity {

    @CreatedDate // 3. Spring Boot pondrá la fecha de creación aquí
    @Column(name = "fecha_creacion", updatable = false)
    protected LocalDateTime fechaCreacion;

    @LastModifiedDate // 4. Spring Boot actualizará esta fecha en cada update
    @Column(name = "fecha_modificacion")
    protected LocalDateTime fechaModificacion;
}