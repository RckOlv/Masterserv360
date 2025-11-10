// Esta es la interfaz para ENVIAR los filtros
export interface ProductoPublicoFiltroDTO {
  nombre?: string;
  categoriaIds?: number[];
  precioMin?: number;
  precioMax?: number;
  soloConStock?: boolean;
}