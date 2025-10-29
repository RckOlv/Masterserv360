// src/app/models/detalle-pedido.model.ts

export interface DetallePedidoDTO {
  id?: number;
  productoId: number;       // Requerido para crear
  cantidad: number;         // Requerido para crear
  
  // --- CORRECCIÓN ---
  // Estos campos son opcionales en el DTO de creación,
  // ya que el backend los calcula o los ignora.
  // Son útiles para *recibir* datos (GET).
  productoNombre?: string;
  productoCodigo?: string;
  precioUnitario?: number; // Ya no es 'number', es 'number | undefined'
  subtotal?: number;
}