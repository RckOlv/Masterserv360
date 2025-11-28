// src/app/models/regla-puntos.model.ts
import { RecompensaDTO } from "./recompensa.model"; // <-- Mentor: IMPORTAR

export interface ReglaPuntosDTO {
  id?: number;
  montoGasto: number;
  puntosGanados: number;
  caducidadPuntosMeses?: number;
  descripcion: string;

  // --- Mentor: INICIO DE LA MODIFICACIÓN ---
  equivalenciaPuntos?: number; // (Lo dejamos opcional por compatibilidad)
  recompensas: RecompensaDTO[]; // <-- AÑADIDO: Lista de premios
  // --- Mentor: FIN DE LA MODIFICACIÓN ---

  // Campos de solo lectura (opcionales al enviar)
  estadoRegla?: string;
  fechaInicioVigencia?: string; 
}