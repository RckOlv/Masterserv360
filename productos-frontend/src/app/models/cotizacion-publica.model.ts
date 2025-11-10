import { ItemCotizacionPublicoDTO } from "./item-cotizacion-publico.model";

export interface CotizacionPublicaDTO {
  token: string;
  proveedorNombre: string;
  fechaSolicitud: string; // (ISO 8601 string)
  items: ItemCotizacionPublicoDTO[];
  fechaEntregaOfertada: string | null; // (ISO 8601 string), vendrá null
}