import { DetalleVentaDTO } from "./detalle-venta.model"; 

export type EstadoVenta = 'COMPLETADA' | 'CANCELADA' | 'PENDIENTE'; 

export interface VentasPorDiaDTO {
  fecha: string; 
  total: number;
}

export interface TopProductoDTO {
  productoId: number;
  nombre: string;
  cantidadVendida: number;
}

export interface VentaDTO {
  // --- Campos para ENVIAR al backend (POST) ---
  clienteId: number;
  detalles: DetalleVentaDTO[]; 

  // --- Campos que vienen del backend (GET) ---
  id?: number;
  fechaVenta?: string; 
  estado?: EstadoVenta;
  totalVenta?: number; // El total FINAL (con descuento aplicado)
  
  clienteNombre?: string; 
  vendedorNombre?: string; 
  fechaCreacion?: string;
  fechaModificacion?: string;
  comprobantePdf?: string;
  
  codigoCupon?: string | null;

  // --- MENTOR: CAMPO NUEVO AGREGADO ---
  montoDescuento?: number; // <--- ESTO ES LO QUE FALTABA
}