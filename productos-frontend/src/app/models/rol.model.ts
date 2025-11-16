import { PermisoDTO } from "./permiso.model"; // Importar el PermisoDTO

export interface RolDTO {
    id?: number;
    nombreRol: string;
    descripcion: string;
    
    // Mentor: ¡NUEVO CAMPO CRÍTICO!
    permisos?: PermisoDTO[]; 
    // Los permisos se envían y reciben en este DTO
}