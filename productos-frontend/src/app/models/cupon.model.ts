// Esta es la interfaz para RECIBIR el cupón generado
// (Coincide con tu CuponDTO.java)
export interface CuponDTO {
  id: number;
  codigo: string;
  descuento: number;
  fechaVencimiento: string; // (LocalDate se convierte en string)
  estado: string; // (VIGENTE, USADO, VENCIDO)
  clienteEmail: string;
}