// Usamos 'class' en lugar de 'interface' 
// para poder inicializarlo vacío en el formulario
export class LoginRequestDTO {
  email: string;
  password: string;

  constructor() {
    this.email = '';
    this.password = '';
  }
}