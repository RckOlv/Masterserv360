import { RecompensaDTO } from "./recompensa.model";


export interface SaldoPuntos {
  saldoPuntos: number;
  valorMonetario: number;
  equivalenciaActual: string;
  recompensasDisponibles: RecompensaDTO[]; // Usamos el DTO compartido
}