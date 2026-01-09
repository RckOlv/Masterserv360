import { CuponDTO } from './cupon.model'; 

export interface ClienteFidelidadDTO {
    clienteId: number;
    nombreCompleto: string;
    puntosAcumulados: number;
    equivalenciaMonetaria: string;
    cuponesDisponibles: CuponDTO[];
}