import { TipoDescuento } from "./enums/tipo-descuento.enum";

export interface RecompensaDTO {
  id?: number;
  descripcion: string;
  puntosRequeridos: number;
  tipoDescuento: TipoDescuento;
  valor: number; 
  
  // --- MENTOR: AGREGADO ---
  stock: number;
  // ------------------------

  reglaPuntosId?: number; 
  categoriaId?: number | null; 
  categoriaNombre?: string; 
}