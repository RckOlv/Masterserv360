// Esta es la interfaz para LEER el historial de compras
export interface VentaResumenDTO {
  id: number;
  fechaVenta: string; // (LocalDateTime se convierte en string)
  estado: string; // (COMPLETADA, CANCELADA)
  totalVenta: number;
  cantidadItems: number;
  codigoCuponUsado: string;
}