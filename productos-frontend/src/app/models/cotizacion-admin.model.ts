import { ItemCotizacionAdminDTO } from "./item-cotizacion-admin.model";

export interface CotizacionAdminDTO {
  id: number;
  proveedorNombre: string;
  proveedorId: number;
  estado: 'PENDIENTE_PROVEEDOR' | 'RECIBIDA' | 'CONFIRMADA_ADMIN' | 'CANCELADA_ADMIN' | 'VENCIDA';
  fechaCreacion: string; // Angular manejará la fecha como string (ISO 8601)
  fechaEntregaOfertada: string | null; // (ISO 8601)
  precioTotalOfertado: number | null;
  esRecomendada: boolean;
  items: ItemCotizacionAdminDTO[];
}