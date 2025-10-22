export interface Producto {
  idProducto?: number;
  codigo: string;
  nombreProducto: string;
  descripcion?: string;
  precioCosto: number;
  precioVenta: number; 
  stockActual: number;
  stockMinimo: number;
  activo: boolean;
  fechaAlta?: string;      // 🔹 Fecha de alta (ISO string)
  imagen?: string;         // 🔹 URL o nombre de archivo
  categoria: { idCategoria: number; nombreCategoria?: string };
}
