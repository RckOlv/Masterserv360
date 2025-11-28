import { TipoDescuento } from "./enums/tipo-descuento.enum";

export interface RecompensaDTO {
  id?: number;
  descripcion: string;
  puntosRequeridos: number;
  tipoDescuento: TipoDescuento;
  valor: number; // (Será 500 para $500, o 20 para 20%)
  
  reglaPuntosId?: number; // Lo necesitamos para crear/actualizar
  categoriaId?: number | null; // Opcional: ID de la categoría
  
  // Solo lectura (para mostrar en la tabla)
  categoriaNombre?: string; 
}