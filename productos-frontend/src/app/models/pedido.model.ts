// src/app/models/pedido.model.ts
import { DetallePedidoDTO } from "./detalle-pedido.model";

// Coincide con el Enum EstadoPedido del backend
export type EstadoPedido = 'PENDIENTE' | 'COMPLETADO' | 'CANCELADO' | 'EN_CAMINO';

export interface PedidoDTO {
  id?: number;
  
  // IDs para crear/editar
  proveedorId: number;
  usuarioId: number; // El empleado que lo crea

  // Campos de solo lectura (para mostrar)
  proveedorRazonSocial?: string;
  usuarioNombre?: string;
  fechaPedido?: string; // Angular manejará LocalDateTime como string
  estado?: EstadoPedido;
  totalPedido?: number;

  // Campos de auditoría (solo lectura)
  fechaCreacion?: string;
  fechaModificacion?: string;

  // Lista de detalles
  detalles: DetallePedidoDTO[];
}