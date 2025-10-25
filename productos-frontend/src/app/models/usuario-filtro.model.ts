export interface UsuarioFiltroDTO {
  nombreOEmail?: string;
  documento?: string;
  rolId?: number;
  estado?: 'ACTIVO' | 'INACTIVO' | 'PENDIENTE' | 'BLOQUEADO' | null; // El 'null' será para 'Todos'
}