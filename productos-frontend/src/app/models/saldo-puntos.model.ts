// Esta es la interfaz para LEER el saldo
// (Coincide con tu SaldoPuntosDTO.java)
export interface SaldoPuntosDTO {
  saldoPuntos: number;
  valorMonetario: number;
  equivalenciaActual: string; // ej: "1 Punto = $1.50 ARS"
}