export interface ItemCotizacionPublicoDTO {
  id: number; // ID del ItemCotizacion
  productoNombre: string;
  productoCodigo: string;
  cantidadSolicitada: number;
  precioUnitarioOfertado: number | null; // Vendrá null, el proveedor lo llena
}