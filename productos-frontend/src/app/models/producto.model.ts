// src/app/models/producto.model.ts
export interface ProductoDTO {
  id?: number; // Es opcional porque al crear, no lo tenemos
  codigo: string;
  nombre: string;
  descripcion?: string; // Opcional
  precioVenta: number; // Angular/TS maneja bien 'number' para BigDecimal
  precioCosto: number;
  imagenUrl?: string; // Opcional
  stockActual: number;
  stockMinimo: number;
  estado?: string; // Opcional
  categoriaId: number;
  categoriaNombre?: string; // Solo lectura, para mostrar
}