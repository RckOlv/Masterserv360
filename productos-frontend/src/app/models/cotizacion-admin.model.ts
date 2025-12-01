import { ItemCotizacionAdminDTO } from "./item-cotizacion-admin.model";

export interface CotizacionAdminDTO {
  id: number;
  proveedorNombre: string;
  proveedorId: number;
  estado: 'PENDIENTE_PROVEEDOR' | 'RECIBIDA' | 'CONFIRMADA_ADMIN' | 'CANCELADA_ADMIN' | 'VENCIDA';
  fechaCreacion: string;
  fechaEntregaOfertada: string | null;
  precioTotalOfertado: number | null;
  esRecomendada: boolean;
  // --- MENTOR: NUEVO CAMPO ---
  observacionAnalisis: string; 
  // ---------------------------
  items: ItemCotizacionAdminDTO[];
}