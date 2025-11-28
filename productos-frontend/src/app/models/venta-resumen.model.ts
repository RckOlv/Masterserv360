// Importamos el tipo EstadoVenta para reusarlo
import { EstadoVenta } from "./venta.model";

export interface VentaResumenDTO {
    id: number;
    fechaVenta: string;
    totalVenta: number;
    estado: EstadoVenta;
    
    // Campos calculados que vienen del backend
    clienteNombre?: string;
    cantidadItems: number;
    codigoCuponUsado?: string;
}