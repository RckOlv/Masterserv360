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
            // 1. Recuperar Venta con todos sus detalles
            Venta venta = ventaRepository.findByIdWithDetails(event.getVentaId())
                    .orElseThrow(() -> new RuntimeException("Venta no encontrada ID: " + event.getVentaId()));

            // 2. Generar PDF
            byte[] pdf = pdfService.generarComprobanteVenta(venta);
            if (pdf == null || pdf.length == 0) {
                logger.error("❌ Error: PDF generado vacío para Venta #{}", venta.getId());
                return;
            }

            // 3. Preparar Contexto Email (AQUÍ FALTABAN DATOS)
            Context context = new Context();
            context.setVariable("clienteNombre", venta.getCliente().getNombre());
            
            // Total
            BigDecimal total = venta.getTotalVenta() != null ? venta.getTotalVenta() : BigDecimal.ZERO;
            context.setVariable("totalVenta", String.format("$%.2f", total));
            
            // Datos Básicos
            context.setVariable("idVenta", venta.getId());
            context.setVariable("fechaVenta", venta.getFechaVenta());

            // --- MENTOR: VARIABLES DE DESCUENTO AGREGADAS ---
            BigDecimal descuento = venta.getMontoDescuento() != null ? venta.getMontoDescuento() : BigDecimal.ZERO;
            context.setVariable("montoDescuento", String.format("$%.2f", descuento));
            
            // Pasamos el objeto booleano para saber si mostrar la fila de descuento en el HTML
            context.setVariable("hayDescuento", descuento.compareTo(BigDecimal.ZERO) > 0);

            // Código de cupón (si existe)
            if (venta.getCupon() != null) {
                context.setVariable("codigoCupon", venta.getCupon().getCodigo());
            } else {
                context.setVariable("codigoCupon", "");
            }
            // ------------------------------------------------

            // 4. Procesar Template HTML
            String html = templateEngine.process("email-comprobante", context);

            // 5. Enviar
            emailService.enviarEmailConAdjunto(
                    venta.getCliente().getEmail(),
                    "Comprobante de compra #" + venta.getId(),
                    html,
                    pdf, 
                    "Comprobante-Masterserv-" + venta.getId() + ".pdf"
            );
            
            logger.info("✅ Email con comprobante enviado a {}", venta.getCliente().getEmail());

        } catch (Exception e) {
            // Si falla aquí, verás el error en la consola
            logger.error("🔴 Error crítico enviando email de venta #{}: {}", event.getVentaId(), e.getMessage(), e);
        }
    }
}