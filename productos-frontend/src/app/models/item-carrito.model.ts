// src/app/models/item-carrito.model.ts

export interface ItemCarritoDTO {
  id: number; // ID del *ItemCarrito* (importante para quitar/actualizar)
  productoId: number;
  productoNombre: string;
  productoCodigo: string;
  precioUnitarioVenta: number; // Precio de venta del producto
  cantidad: number;
  subtotal: number; // Calculado (precio * cantidad)
  stockDisponible: number; // Stock actual del producto
  productoCategoriaId: number;
}