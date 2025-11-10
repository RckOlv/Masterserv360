import { ItemOfertaDTO } from "./item-oferta.model";

export interface OfertaProveedorDTO {
  fechaEntregaOfertada: string; // (ISO 8601 string, ej: "2025-11-20")
  items: ItemOfertaDTO[];
}