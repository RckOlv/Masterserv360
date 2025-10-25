import { PermisoDTO } from "./permiso.model"; // Si usas permisos en el futuro

export interface RolDTO {
  id?: number;
  nombreRol: string; // Ej: 'ROLE_ADMIN', 'ROLE_VENDEDOR'
  descripcion?: string;
  // permisos?: PermisoDTO[]; // Comentar si no tienes PermisoDTO todavía
}