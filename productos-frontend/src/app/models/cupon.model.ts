// Mentor: Importamos el nuevo Enum
import { TipoDescuento } from "./enums/tipo-descuento.enum";

// Esta es la interfaz para RECIBIR el cupón generado (V2)
export interface CuponDTO {
  id: number;
  codigo: string;
  fechaVencimiento: string; // (LocalDate se convierte en string)
  estado: string; // (VIGENTE, USADO, VENCIDO)
  clienteEmail: string;

  // --- Mentor: INICIO DE LA MODIFICACIÓN (V2) ---
  // (Reemplazamos 'descuento' por estos campos)
  
  valor: number; // (Ej: 500 o 20)
  
  tipoDescuento: TipoDescuento; // (FIJO o PORCENTAJE)
  
  categoriaId?: number | null; // (Opcional)
  
  categoriaNombre?: string; // (Opcional)
  // --- Mentor: FIN DE LA MODIFICACIÓN ---
}