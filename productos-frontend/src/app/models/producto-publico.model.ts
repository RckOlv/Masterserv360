// Esta es la interfaz para LEER el catálogo
export interface ProductoPublicoDTO {
  id: number;
  nombre: string;
  descripcion: string;
  precioVenta: number;
  stockActual: number;
  nombreCategoria: string;
  imagenUrl: string;
}