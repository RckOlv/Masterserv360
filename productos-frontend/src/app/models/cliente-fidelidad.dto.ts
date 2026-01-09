import { CuponDTO } from './cupon.model'; 
import { RecompensaDTO } from './recompensa.model';

export interface ClienteFidelidadDTO {
    clienteId: number;
    nombreCompleto: string;
    puntosAcumulados: number;
    equivalenciaMonetaria: string;
    cuponesDisponibles: CuponDTO[];
    recompensasAlcanzables: RecompensaDTO[];
}