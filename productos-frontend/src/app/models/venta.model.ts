import { DetalleVentaDTO } from "./detalle-venta.model"; // Importa el DTO del detalle

// Coincide con el Enum EstadoVenta del backend
export type EstadoVenta = 'COMPLETADA' | 'CANCELADA' | 'PENDIENTE'; // Ajusta si tienes otros estados

export interface VentasPorDiaDTO {
  fecha: string; // El backend envía LocalDate, Angular lo recibe como string
  total: number;
}

/**
 * DTO para el gráfico de productos top (TopProductoDTO del backend)
 */
export interface TopProductoDTO {
  productoId: number;
  nombre: string;
  cantidadVendida: number;
}

export interface VentaDTO {
  // --- Campos para ENVIAR al backend (POST) ---
  clienteId: number;
  // vendedorId NO se envía, se obtiene del token en backend
  detalles: DetalleVentaDTO[]; // Array de detalles (solo con productoId y cantidad)

  // --- Campos que vienen del backend (GET) ---
  id?: number;
  fechaVenta?: string; // Angular maneja LocalDateTime como string
  estado?: EstadoVenta;
  totalVenta?: number;
  clienteNombre?: string; // Nombre/Apellido del cliente
  vendedorNombre?: string; // Nombre/Apellido del vendedor
  fechaCreacion?: string;
  fechaModificacion?: string;
  comprobantePdf?: string;
  codigoCupon?: string | null;
}