// src/app/models/producto.model.ts
export interface ProductoDTO {
  id?: number; // Es opcional porque al crear, no lo tenemos
  codigo: string;
  nombre: string;
  descripcion?: string; // Opcional
  precioVenta: number; 
  precioCosto: number;
  imagenUrl?: string; // Opcional
  stockActual: number;
  stockMinimo: number;
  
  // --- Mentor: INICIO DE LA MODIFICACIÓN ---
  loteReposicion: number; // <-- ¡CAMPO AÑADIDO!
  // --- Mentor: FIN DE LA MODIFICACIÓN ---

  estado?: string; // Opcional
  categoriaId: number;
  categoriaNombre?: string; // Solo lectura, para mostrar
}