// src/app/models/producto-filtro.model.ts
export interface ProductoFiltroDTO {
  nombre?: string;
  codigo?: string;
  categoriaId?: number;
  precioMax?: number;
  conStock?: boolean;
  estado?: string;
}