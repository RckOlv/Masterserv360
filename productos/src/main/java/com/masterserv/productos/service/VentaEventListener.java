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
        logger.info("Reaccionando al evento VentaRealizadaEvent para Venta ID: {}", event.getVentaId());
        
        try {
            Venta ventaCompleta = ventaRepository.findByIdWithDetails(event.getVentaId())
                    .orElseThrow(() -> new RuntimeException("Venta no encontrada para PDF: " + event.getVentaId()));

            // --- INICIO DE LA MODIFICACIÓN ---
            // Generamos el PDF
            byte[] pdf = pdfService.generarComprobanteVenta(ventaCompleta);

            // ¡LA PRUEBA DE TINTA!
            // Vamos a loguear el tamaño del PDF ANTES de enviarlo.
            if (pdf != null && pdf.length > 0) {
                logger.info("PDF generado para Venta #{}. Tamaño: {} bytes.", event.getVentaId(), pdf.length);
            } else {
                logger.warn("PDF para Venta #{} NO se generó o está vacío (bytes is null or empty).", event.getVentaId());
            }

            // Llamamos al método de envío
            enviarComprobantePorEmail(ventaCompleta, pdf);
            // --- FIN DE LA MODIFICACIÓN ---

            // (El log de éxito lo moví al método de abajo para ser más precisos)

        } catch (Exception e) {
            logger.error("⚠️ La Venta #{} se guardó, pero falló la generación/envío de PDF/Email asíncrono: {}", 
                         event.getVentaId(), e.getMessage(), e);
        }
    }

    private void enviarComprobantePorEmail(Venta venta, byte[] pdf) {
        // Hacemos una última validación aquí
        if (pdf == null || pdf.length == 0) {
            logger.error("Error al enviar email: El PDF está nulo o vacío. Abortando envío de adjunto.");
            // (Podríamos enviar el email SIN adjunto aquí si quisiéramos)
            return; 
        }

        try {
            Context context = new Context();
            context.setVariable("clienteNombre", venta.getCliente().getNombre());
            // Mentor: ¡CUIDADO! El total de la compra no estaba en el template. 
            // Lo añado aquí para que coincida con tu imagen de MailHog.
            context.setVariable("totalVenta", String.format("$%.2f", venta.getTotalVenta()));

            String html = templateEngine.process("email-comprobante", context);
            
            emailService.enviarEmailConAdjunto(
                    venta.getCliente().getEmail(),
                    "Comprobante de compra #" + venta.getId(),
                    html,
                    pdf, // El byte[] del PDF
                    "Comprobante-Masterserv-" + venta.getId() + ".pdf"
            );
            
            // Este log solo se ejecuta si emailService.enviarEmailConAdjunto NO lanza una excepción
            logger.info("PDF y Email para Venta #{} procesados y enviados exitosamente.", venta.getId());

        } catch (Exception e) {
            logger.error("Error al construir o enviar el email para Venta #{}: {}", venta.getId(), e.getMessage());
        }
    }
}