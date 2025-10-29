// src/app/models/carrito.model.ts
import { ItemCarritoDTO } from "./item-carrito.model"; // Importa el DTO del item

export interface CarritoDTO {
  id: number; // El ID del carrito
  vendedorId: number; // El ID del vendedor dueño
  items: ItemCarritoDTO[]; // La lista de items
  totalCarrito: number; // El total calculado
  cantidadItems: number; // La cantidad total de unidades
}