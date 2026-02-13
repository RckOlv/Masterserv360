package com.masterserv.productos.controller;

import com.masterserv.productos.dto.DetalleComparativaDTO;
import com.masterserv.productos.dto.ResumenProductoCompraDTO;
import com.masterserv.productos.dto.SeleccionCompraDTO;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.repository.ItemCotizacionRepository;
import com.masterserv.productos.repository.UsuarioRepository;
import com.masterserv.productos.service.PedidoService;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/compras")
@PreAuthorize("hasAnyRole('ADMIN', 'COMPRAS')") // Ajusta según tus roles
public class ComprasController {

    @Autowired
    private ItemCotizacionRepository itemRepo;

	@Autowired
    private PedidoService pedidoService;

    @Autowired 
    private UsuarioRepository usuarioRepository;

    @GetMapping("/productos-cotizados")
    public ResponseEntity<List<ResumenProductoCompraDTO>> getProductosCotizados() {
        return ResponseEntity.ok(itemRepo.findProductosEnCotizacionesRecibidas());
    }

    @GetMapping("/comparativa/{productoId}")
    public ResponseEntity<List<DetalleComparativaDTO>> getComparativaProducto(@PathVariable Long productoId) {
        return ResponseEntity.ok(itemRepo.findComparativaPorProducto(productoId));
    }

	@PostMapping("/generar-masivo")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPRAS', 'VENDEDOR')")
    public ResponseEntity<?> generarPedidosMasivos(@RequestBody SeleccionCompraDTO dto, Authentication auth) {
        
        // Obtenemos el usuario autenticado (token JWT) para asignarlo como creador del pedido
        String emailActual = auth.getName();
        Usuario admin = usuarioRepository.findByEmail(emailActual)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Llamamos al servicio
        Map<String, Object> resultado = pedidoService.generarPedidosMasivos(dto.getItemCotizacionIds(), admin.getId());
        
        return ResponseEntity.ok(resultado);
    }
}