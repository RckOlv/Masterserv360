// src/app/models/usuario.model.ts
import { RolDTO } from "./rol.model"; 

// Esta es la interfaz que define los datos que vienen del backend
export interface UsuarioDTO {
  id?: number; // Opcional al crear
  nombre: string;
  apellido: string;
  email: string;
  
  passwordHash?: string; // Solo para enviar al crear/actualizar
  
  documento?: string;       
  telefono?: string;       
  
  // --- ¡AQUÍ ESTÁ LA CORRECCIÓN! ---
  // Ahora permitimos todos los estados del backend
  estado?: 'ACTIVO' | 'INACTIVO' | 'PENDIENTE' | 'BLOQUEADO'; 
  
  roles: RolDTO[]; // Lista de roles
  
  tipoDocumentoId?: number; 
  // tipoDocumentoNombre?: string; // Podríamos añadir esto si el Mapper lo enviara
}