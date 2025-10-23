// src/app/models/categoria.model.ts
export interface CategoriaDTO {
  id?: number; // El ID es opcional al crear
  nombre: string;
  descripcion?: string; // Opcional
  // estado?: string; // Podríamos añadirlo si el backend lo devuelve (para soft delete)
}