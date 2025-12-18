import { EstadoPedido } from '../models/enums/estado-pedido.enum'; // Asegúrate de tener este enum o usa string
import { DetallePedidoDTO } from './detalle-pedido.model'; // Importamos el que ya tienes

export interface PedidoDetallado {
  id: number;
  fechaPedido: string; // El backend manda LocalDateTime como ISO string
  estado: EstadoPedido | string;
  totalPedido: number;
  
  // Datos del Proveedor
  proveedorId: number;
  proveedorRazonSocial: string;
  proveedorCuit: string;
  proveedorEmail: string;
  proveedorTelefono?: string;
  
  // Datos del Usuario
  usuarioSolicitante: string; // El nombre del empleado

  // Lista de items (Reutilizamos tu DTO)
  detalles: DetallePedidoDTO[];
}