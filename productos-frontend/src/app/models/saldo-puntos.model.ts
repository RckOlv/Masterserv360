export interface Recompensa {
  id: number;
  descripcion: string;
  puntosRequeridos: number;
  tipoDescuento: 'PORCENTAJE' | 'FIJO';
  valor: number;
  categoriaNombre?: string;
  // --- MENTOR: AGREGADO CAMPO STOCK ---
  stock: number;
}

export interface SaldoPuntos {
  saldoPuntos: number;
  valorMonetario: number;
  equivalenciaActual: string;
  recompensasDisponibles: Recompensa[];
}