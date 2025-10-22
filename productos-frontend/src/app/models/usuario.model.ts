import { Rol } from './rol.model';

export interface Usuario {
  idUsuario?: number;
  nombre: string;
  apellido: string;
  documento: string;
  email: string;
  password?: string;
  rol?: Rol;
  estado?: boolean;
}
