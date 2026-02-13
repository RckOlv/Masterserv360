export interface ResumenProductoCompra {
    productoId: number;
    nombre: string;
    codigo: string;
    imagenUrl: string;
    cantidadCotizaciones: number;
    mejorPrecio: number;
}

export interface DetalleComparativa {
    cotizacionId: number;
    proveedorNombre: string;
    precioOferta: number;
    cantidadOfrecida: number;
    fechaEntrega: string;
    esRecomendada: boolean;
}