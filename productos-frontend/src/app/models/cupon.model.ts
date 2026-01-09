
import { TipoDescuento } from "./enums/tipo-descuento.enum";

export interface CuponDTO {
  id: number;
  codigo: string;
  fechaVencimiento: string; 
  estado: string; 
  clienteEmail: string;

  valor: number; 
  
  tipoDescuento: TipoDescuento; 
  
  categoriaId?: number | null; 
  
  categoriaNombre?: string; 
}