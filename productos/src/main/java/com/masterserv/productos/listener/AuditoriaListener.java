package com.masterserv.productos.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.masterserv.productos.config.BeanUtil;
import com.masterserv.productos.entity.AuditableEntity;
import com.masterserv.productos.entity.Auditoria;
import com.masterserv.productos.service.AuditoriaService;
import jakarta.persistence.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

public class AuditoriaListener {

    @PostPersist
    public void onPersist(Object entity) { guardarLog(entity, "CREAR"); }

    @PostUpdate
    public void onUpdate(Object entity) { guardarLog(entity, "ACTUALIZAR"); }

    @PostRemove
    public void onRemove(Object entity) { guardarLog(entity, "ELIMINAR"); }

    private void guardarLog(Object entity, String accion) {
        if (entity instanceof Auditoria) return;

        try {
            String usuario = "Sistema / Anónimo";
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                usuario = auth.getName();
            }

            String nombreEntidad = entity.getClass().getSimpleName();
            String idEntidad = "N/A";
            
            try {
                Method method = entity.getClass().getMethod("getId");
                Object idObj = method.invoke(entity);
                if (idObj != null) idEntidad = idObj.toString();
            } catch (Exception ignored) {}

            // --- PREPARAR DATOS ---
            String valorAnterior = null;
            String valorNuevo = null;
            ObjectMapper mapper = BeanUtil.getBean(ObjectMapper.class);

            if (entity instanceof AuditableEntity) {
                AuditableEntity auditable = (AuditableEntity) entity;
                
                if ("ACTUALIZAR".equals(accion)) {
                    valorAnterior = auditable.getEstadoAnterior(); // JSON guardado al cargar
                    valorNuevo = mapper.writeValueAsString(entity); // JSON actual
                } else if ("ELIMINAR".equals(accion)) {
                    valorAnterior = mapper.writeValueAsString(entity);
                } else if ("CREAR".equals(accion)) {
                    valorNuevo = mapper.writeValueAsString(entity);
                }
            }

            // Detalle Corto (Solo quién y qué)
            String detalleCorto = String.format("%s en %s #%s", accion, nombreEntidad, idEntidad);

            // --- CREAR AUDITORIA ---
            Auditoria log = new Auditoria();
            log.setEntidad(nombreEntidad);
            log.setEntidadId(idEntidad);
            log.setAccion(accion);
            log.setUsuario(usuario);
            log.setFecha(LocalDateTime.now());
            log.setDetalle(detalleCorto);
            
            // Guardamos las columnas nuevas
            log.setValorAnterior(valorAnterior);
            log.setValorNuevo(valorNuevo);

            AuditoriaService service = BeanUtil.getBean(AuditoriaService.class);
            service.guardar(log);

        } catch (Exception e) {
            System.err.println(">>> AUDITORIA ERROR: " + e.getMessage());
        }
    }
}