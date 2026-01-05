import { RecompensaDTO } from "./recompensa.model";

export interface Recompensa {
  id: number;
  descripcion: string;
  puntosRequeridos: number;
  tipoDescuento: 'PORCENTAJE' | 'FIJO';
  valor: number;
  categoriaNombre?: string;
  stock: number;
}

export interface SaldoPuntos {
  saldoPuntos: number;
  valorMonetario: number;
  equivalenciaActual: string;
  recompensasDisponibles: RecompensaDTO[];
}