// src/app/models/detalle-pedido.model.ts
export interface DetallePedidoDTO {
  id?: number; // Es 'number' en TS
  productoId: number;
  productoNombre?: string; // Para mostrar
  productoCodigo?: string; // Para mostrar
  cantidad: number;
  precioUnitario: number; // El precio de costo
  subtotal?: number; // Para mostrar
}