export interface AuthResponseDTO {
  token: string;
  
  // Mentor: Campos añadidos para que coincidan con el DTO del backend
  usuarioId: number;
  email: string;
  roles: string[];
  permisos: string[];
  debeCambiarPassword: boolean;
}