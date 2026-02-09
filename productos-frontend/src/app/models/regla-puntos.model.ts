// src/app/models/regla-puntos.model.ts
import { RecompensaDTO } from "./recompensa.model"; // <-- Mentor: IMPORTAR

export interface ReglaPuntosDTO {
  id?: number;
  montoGasto: number;
  puntosGanados: number;
  caducidadPuntosMeses?: number;
  descripcion: string;

  equivalenciaPuntos?: number; // (Lo dejamos opcional por compatibilidad)

  estadoRegla?: string;
  fechaInicioVigencia?: string; 
  fechaVencimiento?: string;
}