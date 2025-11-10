// Esta es la interfaz para ACTUALIZAR el perfil
export interface ClientePerfilUpdateDTO {
  nombre: string;
  apellido: string;
  telefono: string;
  tipoDocumentoId: number;
  documento: string;
}