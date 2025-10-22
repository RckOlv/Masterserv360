export interface LoginResponse {
  status: string;
  token?: string;
  usuario?: {
    idUsuario: number;
    nombre: string;
    apellido: string;
    email: string;
    documento: string;
    estado: boolean;
    rol: {
      idRol: number;
      nombre: string;
      permisos: string[];
    } | string;
  };
}
