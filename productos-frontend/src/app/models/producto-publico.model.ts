export interface ProductoPublicoDTO {
  id: number;
  nombre: string;
  codigo: string;
  descripcion: string;
  precioVenta: number;
  stockActual: number;
  imagenUrl?: string | null; 
  nombreCategoria: string;
  stockMinimo: number;
}