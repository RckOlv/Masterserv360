package com.masterserv.productos.service;

import com.masterserv.productos.entity.Venta;
import com.masterserv.productos.event.VentaRealizadaEvent;
import com.masterserv.productos.repository.VentaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;

@Component
public class VentaEventListener {

    private static final Logger logger = LoggerFactory.getLogger(VentaEventListener.class);

    @Autowired private VentaRepository ventaRepository;
    @Autowired private PdfService pdfService;
    @Autowired private EmailService emailService;
    @Autowired private TemplateEngine templateEngine;

    @Async
    @TransactionalEventListener
    public void handleVentaRealizada(VentaRealizadaEvent event) {
        logger.info("-> 📨 [EVENTO] Procesando venta #{} para envío de email...", event.getVentaId());
        
        try {
            Venta venta = ventaRepository.findByIdWithDetails(event.getVentaId())
                    .orElseThrow(() -> new RuntimeException("Venta no encontrada ID: " + event.getVentaId()));

            byte[] pdf = pdfService.generarComprobanteVenta(venta);
            if (pdf == null || pdf.length == 0) {
                logger.error("❌ Error: PDF generado vacío para Venta #{}", venta.getId());
                return;
            }

            Context context = new Context();
            context.setVariable("clienteNombre", venta.getCliente().getNombre());
            
            // --- CORRECCIÓN MENTOR: Pasamos BigDecimal PURO (sin String.format) ---
            // Así Thymeleaf puede hacer sus cálculos sin explotar
            BigDecimal total = venta.getTotalVenta() != null ? venta.getTotalVenta() : BigDecimal.ZERO;
            context.setVariable("totalVenta", total); 
            // ---------------------------------------------------------------------
            
            context.setVariable("idVenta", venta.getId());
            context.setVariable("fechaVenta", venta.getFechaVenta());

            BigDecimal descuento = venta.getMontoDescuento() != null ? venta.getMontoDescuento() : BigDecimal.ZERO;
            context.setVariable("montoDescuento", descuento); // También pasamos BigDecimal puro aquí
            
            context.setVariable("hayDescuento", descuento.compareTo(BigDecimal.ZERO) > 0);

            if (venta.getCupon() != null) {
                context.setVariable("codigoCupon", venta.getCupon().getCodigo());
            } else {
                context.setVariable("codigoCupon", "");
            }

            String html = templateEngine.process("email-comprobante", context);

            emailService.enviarEmailConAdjunto(
                    venta.getCliente().getEmail(),
                    "Comprobante de compra #" + venta.getId(),
                    html,
                    pdf, 
                    "Comprobante-Masterserv-" + venta.getId() + ".pdf"
            );
            
            logger.info("✅ Email con comprobante enviado a {}", venta.getCliente().getEmail());

        } catch (Exception e) {
            logger.error("🔴 Error crítico enviando email de venta #{}: {}", event.getVentaId(), e.getMessage(), e);
        }
    }
}