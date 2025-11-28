export interface Recompensa {
  id: number;
  descripcion: string;
  puntosRequeridos: number;
  tipoDescuento: 'PORCENTAJE' | 'FIJO';
  valor: number;
  categoriaNombre?: string;
}

export interface SaldoPuntos {
  saldoPuntos: number;
  valorMonetario: number;
  equivalenciaActual: string;
  // Aquí agregamos la lista que viene del backend
  recompensasDisponibles: Recompensa[];
}