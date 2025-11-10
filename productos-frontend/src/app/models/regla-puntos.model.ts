export interface ReglaPuntosDTO {
  id?: number;
  montoGasto: number;
  puntosGanados: number;
  equivalenciaPuntos: number; // BigDecimal es 'number'
  caducidadPuntosMeses?: number;
  descripcion: string;

  // Campos de solo lectura (opcionales al enviar)
  estadoRegla?: string;
  fechaInicioVigencia?: string; // LocalDateTime es 'string'
}