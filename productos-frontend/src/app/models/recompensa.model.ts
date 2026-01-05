import { TipoDescuento } from "./enums/tipo-descuento.enum";

export interface RecompensaDTO {
  id?: number;
  descripcion: string;
  puntosRequeridos: number;
  tipoDescuento: TipoDescuento;
  valor: number;
  stock: number;
  
  activo?: boolean;
  categoriaId?: number | null;
  categoriaNombre?: string;
}