// Esta es la interfaz para LEER el perfil
export interface ClientePerfilDTO {
  id: number;
  email: string;
  nombre: string;
  apellido: string;
  telefono: string;
  tipoDocumentoId: number; 
  tipoDocumento: string; 
  documento: string;
}