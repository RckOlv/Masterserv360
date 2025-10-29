export interface DetalleVentaDTO {
   // --- Campos para ENVIAR al backend (POST en VentaDTO) ---
   productoId: number;
   cantidad: number;

   // --- Campos que vienen del backend (GET Venta por ID) ---
   id?: number;
   productoNombre?: string;
   productoCodigo?: string;
   precioUnitario?: number; // Precio al que SE VENDIÓ (congelado)
   subtotal?: number; // Calculado
}