import { EstadoVenta } from "./venta.model"; // Importa el tipo EstadoVenta

export interface VentaFiltroDTO {
  clienteId?: number | null; // ID numérico o null
  vendedorId?: number | null; // ID numérico o null
  fechaDesde?: string | null; // Formato 'YYYY-MM-DD' o null
  fechaHasta?: string | null; // Formato 'YYYY-MM-DD' o null
  estado?: EstadoVenta | null; // Tipo EstadoVenta o null
}