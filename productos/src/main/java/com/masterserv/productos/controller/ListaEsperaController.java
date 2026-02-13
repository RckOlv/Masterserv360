package com.masterserv.productos.controller;

import com.masterserv.productos.dto.AddListaEsperaDTO;
import com.masterserv.productos.dto.ListaEsperaDTO;
import com.masterserv.productos.entity.ListaEspera;
import com.masterserv.productos.entity.Producto;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.enums.EstadoListaEspera;
import com.masterserv.productos.repository.ListaEsperaRepository;
import com.masterserv.productos.repository.ProductoRepository;
import com.masterserv.productos.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/lista-espera")
@CrossOrigin(origins = "") // Permitimos conexión desde Angular
public class ListaEsperaController {

    private static final Logger logger = LoggerFactory.getLogger(ListaEsperaController.class);

    @Autowired
    private ListaEsperaRepository listaEsperaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Endpoint para que un cliente se anote en la lista de espera de un producto.
     */
    @PostMapping("/unirse")
    public ResponseEntity<?> unirseALista(@RequestBody AddListaEsperaDTO dto) {
        logger.info("-> Solicitud de lista de espera: Usuario ID {} para Producto ID {}", dto.getUsuarioId(), dto.getProductoId());

        try {
            // 1. Validar existencia de Usuario y Producto
            Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            Producto producto = productoRepository.findById(dto.getProductoId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            // 2. Validar Stock (Opcional: Si ya hay stock, avisarle al front que lo compre directo)
            if (producto.getStockActual() > 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("mensaje", "¡Buena noticia! Este producto ya tiene stock, puedes comprarlo ahora."));
            }

            // 3. Validar Duplicados (Usamos el método que creamos en el Repo)
            boolean yaEstaEnEspera = listaEsperaRepository.existsByUsuarioIdAndProductoIdAndEstado(
                    dto.getUsuarioId(), 
                    dto.getProductoId(), 
                    EstadoListaEspera.PENDIENTE
            );

            if (yaEstaEnEspera) {
                return ResponseEntity.status(HttpStatus.CONFLICT) // 409 Conflict
                        .body(Map.of("mensaje", "Ya estás anotado en la lista de espera para este producto."));
            }

            // 4. Crear y Guardar
            ListaEspera nuevaEspera = new ListaEspera();
            nuevaEspera.setUsuario(usuario);
            nuevaEspera.setProducto(producto);
            nuevaEspera.setFechaInscripcion(LocalDate.now());
            nuevaEspera.setEstado(EstadoListaEspera.PENDIENTE); // Usamos el Enum

            listaEsperaRepository.save(nuevaEspera);

            logger.info("-> ✅ Usuario {} agregado a lista de espera para '{}'", usuario.getEmail(), producto.getNombre());
            
            return ResponseEntity.ok(Map.of("mensaje", "¡Listo! Te avisaremos por correo cuando haya stock."));

        } catch (RuntimeException e) {
            logger.error("Error al unir a lista de espera: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error inesperado en lista de espera", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Ocurrió un error al procesar tu solicitud."));
        }
    }
    
    /**
     * Endpoint opcional: Para que el botón del Frontend sepa si ya está anotado
     * GET /api/lista-espera/check?usuarioId=1&productoId=5
     */
    @GetMapping("/check")
    public ResponseEntity<?> verificarEstado(@RequestParam Long usuarioId, @RequestParam Long productoId) {
        boolean estaPendiente = listaEsperaRepository.existsByUsuarioIdAndProductoIdAndEstado(
                usuarioId, productoId, EstadoListaEspera.PENDIENTE
        );
        return ResponseEntity.ok(Map.of("estaEnEspera", estaPendiente));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarDeLista(@PathVariable Long id) {
        if (!listaEsperaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        listaEsperaRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<?> obtenerTodos() {
        try {
            var lista = listaEsperaRepository.findAll().stream().map(item -> {
                ListaEsperaDTO dto = new ListaEsperaDTO();
                dto.setId(item.getId());
                dto.setFechaSolicitud(item.getFechaSolicitud() != null ? item.getFechaSolicitud().toString() : item.getFechaInscripcion().toString());
                dto.setEstado(item.getEstado().name());
                
                // Mapeo de Usuario
                if (item.getUsuario() != null) {
                    dto.setUsuarioNombre(item.getUsuario().getNombre());
                    dto.setUsuarioApellido(item.getUsuario().getApellido());
                    dto.setUsuarioTelefono(item.getUsuario().getTelefono());
                    dto.setUsuarioEmail(item.getUsuario().getEmail());
                }
                
                // Mapeo de Producto
                if (item.getProducto() != null) {
                    dto.setProductoNombre(item.getProducto().getNombre());
                    dto.setProductoCodigo(item.getProducto().getCodigo());
                }
                
                return dto;
            }).toList();

            return ResponseEntity.ok(lista);
        } catch (Exception e) {
            logger.error("Error al listar lista de espera", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}