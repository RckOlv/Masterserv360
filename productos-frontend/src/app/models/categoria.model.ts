// src/app/models/categoria.model.ts
export interface CategoriaDTO {
  id?: number; // El ID es opcional al crear
  nombre: string;
  descripcion?: string;
  estado?: string; 
}