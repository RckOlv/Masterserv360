package com.masterserv.productos.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.masterserv.productos.config.BeanUtil;
import com.masterserv.productos.entity.AuditableEntity;
import com.masterserv.productos.entity.Auditoria;
import com.masterserv.productos.entity.Producto; // ✅ Importante
import com.masterserv.productos.service.AuditoriaService;
import jakarta.persistence.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Map;

public class AuditoriaListener {

    @PostPersist
    public void onPersist(Object entity) { guardarLog(entity, "CREAR"); }

    @PostUpdate
    public void onUpdate(Object entity) { guardarLog(entity, "ACTUALIZAR"); }

    @PostRemove
    public void onRemove(Object entity) { guardarLog(entity, "ELIMINAR"); }

    private void guardarLog(Object entity, String accion) {
        // Evitar bucles infinitos auditando la propia tabla de auditoría
        if (entity instanceof Auditoria) return;

        try {
            // 1. Obtener Usuario Actual
            String usuario = "Sistema / Anónimo";
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                usuario = auth.getName();
            }

            // 2. Obtener Identificadores Básicos
            String nombreEntidad = entity.getClass().getSimpleName();
            String idEntidad = "N/A";
            
            try {
                Method method = entity.getClass().getMethod("getId");
                Object idObj = method.invoke(entity);
                if (idObj != null) idEntidad = idObj.toString();
            } catch (Exception ignored) {}

            // 3. Preparar JSONs (Valor Anterior vs Nuevo)
            String valorAnterior = null;
            String valorNuevo = null;
            ObjectMapper mapper = BeanUtil.getBean(ObjectMapper.class);

            if (entity instanceof AuditableEntity) {
                AuditableEntity auditable = (AuditableEntity) entity;
                
                if ("ACTUALIZAR".equals(accion)) {
                    // El valor anterior ya fue capturado al cargar la entidad (@PostLoad en AuditableEntity)
                    valorAnterior = auditable.getEstadoAnterior(); 
                    // El nuevo lo generamos ahora con nuestra función mejorada
                    valorNuevo = generarJsonSeguro(entity, mapper); 
                } else if ("ELIMINAR".equals(accion)) {
                    valorAnterior = generarJsonSeguro(entity, mapper);
                } else if ("CREAR".equals(accion)) {
                    valorNuevo = generarJsonSeguro(entity, mapper);
                }
            }

            // 4. Detalle Corto (Resumen)
            String detalleCorto = String.format("%s en %s #%s", accion, nombreEntidad, idEntidad);

            // 5. Construir y Guardar Auditoría
            Auditoria log = new Auditoria();
            log.setEntidad(nombreEntidad);
            log.setEntidadId(idEntidad);
            log.setAccion(accion);
            log.setUsuario(usuario);
            log.setFecha(LocalDateTime.now());
            log.setDetalle(detalleCorto);
            
            log.setValorAnterior(valorAnterior);
            log.setValorNuevo(valorNuevo);

            AuditoriaService service = BeanUtil.getBean(AuditoriaService.class);
            service.guardar(log);

        } catch (Exception e) {
            // Usamos System.err para no romper el flujo principal si falla la auditoría automática
            System.err.println(">>> AUDITORIA LISTENER ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Método auxiliar para generar un JSON limpio y resolver relaciones problemáticas
     * como la Categoría en Producto.
     */
    private String generarJsonSeguro(Object entity, ObjectMapper mapper) {
        try {
            // 1. Convertimos la entidad a un Map flexible
            // Esto evita problemas directos de serialización y nos deja editar claves
            Map<String, Object> mapaDatos = mapper.convertValue(entity, new TypeReference<Map<String, Object>>() {});

            // 2. Lógica específica: Si es PRODUCTO, arreglamos la categoría
            if (entity instanceof Producto) {
                Producto prod = (Producto) entity;
                String nombreCategoria = "Sin Categoría";
                
                if (prod.getCategoria() != null) {
                    nombreCategoria = prod.getCategoria().getNombre();
                }
                
                // Sobrescribimos el objeto complejo 'categoria' con solo el nombre (String)
                mapaDatos.put("categoria", nombreCategoria);
            }

            // 3. Serializamos el Mapa ya limpio a String JSON
            return mapper.writeValueAsString(mapaDatos);

        } catch (Exception e) {
            return "{\"error\": \"No se pudo serializar: " + e.getMessage() + "\"}";
        }
    }
}