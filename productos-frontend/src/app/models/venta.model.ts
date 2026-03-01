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
  clienteId: number;
  detalles: DetalleVentaDTO[]; 
  id?: number;
  fechaVenta?: string; 
  estado?: EstadoVenta;
  totalVenta?: number; 
  clienteNombre?: string; 
  vendedorNombre?: string; 
  fechaCreacion?: string;
  fechaModificacion?: string;
  comprobantePdf?: string;
  codigoCupon?: string | null;
  montoDescuento?: number; 
  observacionCancelacion?: string | null;
}