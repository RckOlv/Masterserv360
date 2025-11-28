export interface ProductoFiltroDTO {
  nombre?: string | null;
  codigo?: string | null;
  categoriaId?: number | null;
  precioMax?: number | null;
  conStock?: boolean | null;
  estado?: string | null;
  
  // Nuevo campo para el filtro unificado
  estadoStock?: string | null; 
}