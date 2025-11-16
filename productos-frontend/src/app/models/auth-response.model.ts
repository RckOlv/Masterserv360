export interface AuthResponseDTO {
  token: string;
  
  // Mentor: Campos añadidos para que coincidan con el DTO del backend
  usuarioId: number;
  email: string;
  roles: string[];
  permisos: string[];

  // Mentor: El campo 'tipo' (ej: "Bearer") ya no es necesario
  // ya que el token se guarda y se usa con el interceptor.
}