export interface DashboardStatsDTO {
  totalVentasMes: number;
  productosBajoStock: number;
  clientesActivos: number;
  totalVentasHoy: number;
  
  // --- NUEVO: Campo para los pedidos ---
  pedidosEnCamino?: {
      proveedor: string;
      fechaEntrega: string; // Viene como string 'yyyy-MM-dd'
      diasRestantes: number;
  }[];
}