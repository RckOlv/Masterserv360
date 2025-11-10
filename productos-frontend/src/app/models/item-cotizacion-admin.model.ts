export interface ItemCotizacionAdminDTO {
  id: number;
  productoId: number;
  productoNombre: string;
  cantidadSolicitada: number;
  estado: 'PENDIENTE' | 'COTIZADO' | 'NO_DISPONIBLE_PROVEEDOR' | 'CANCELADO_ADMIN';
  precioUnitarioOfertado: number | null;
  subtotalOfertado: number;
}