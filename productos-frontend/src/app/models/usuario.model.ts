export interface UsuarioDTO { // Un DTO para mostrar usuarios (no el de registro)
  id?: number;
  nombre: string;
  apellido: string;
  email: string;
  documento?: string;
  telefono?: string;
  estado?: string; // Podría ser 'ACTIVO', 'INACTIVO'
  roles?: RolDTO[]; // Necesitarás RolDTO
  // No incluyas passwordHash aquí
}

// Necesitarás también RolDTO, PermisoDTO, etc.
export interface RolDTO {
  id?: number;
  nombreRol: string;
}