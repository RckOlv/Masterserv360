export class RegisterRequestDTO {
  nombre: string;
  apellido: string;
  email: string;
  password: string;
  tipoDocumentoId?: number; // Opcional
  documento?: string;       // Opcional
  telefono?: string;        // Opcional

  constructor() {
    this.nombre = '';
    this.apellido = '';
    this.email = '';
    this.password = '';
  }
}