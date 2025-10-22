import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../service/auth.service';
import { mostrarToast } from '../../utils/toast';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { LoginResponse } from '../../models/login-response.model'; // asegúrate de tener este modelo

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './login.html',
  styleUrls: ['./login.css']
})
export class LoginComponent {
  email = '';
  password = '';
  loading = false;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  onSubmit() {
    if (!this.email || !this.password) {
      mostrarToast('Por favor, completa todos los campos.', 'warning');
      return;
    }

    this.loading = true;

    this.authService.login(this.email, this.password).subscribe({
      next: (response: LoginResponse) => {
        this.loading = false;

        if (response.status === 'success' && response.usuario) {
          const usuario = response.usuario;
          // 🔹 Si `rol` es objeto → usamos nombre, si es string → lo usamos directo
          const rol = typeof usuario.rol === 'object' && usuario.rol !== null
            ? usuario.rol.nombre
            : (usuario.rol ?? 'Sin rol');

          const token: string = response.token ?? '';

          if (!token) {
            mostrarToast('No se recibió el token. Intenta nuevamente.', 'danger');
            return;
          }

          // 🔹 Guardar sesión en localStorage
          localStorage.setItem('token', token);
          localStorage.setItem('usuario', JSON.stringify(usuario));

          // 🔹 Mostrar mensaje de bienvenida
          mostrarToast(`Bienvenido ${usuario.nombre} (${rol})`, 'success');

          // 🔹 Redirección según el rol
          switch (rol) {
            case 'ADMIN':
            case 'VENDEDOR':
              this.router.navigate(['/pos/dashboard']);
              break;
            default:
              this.router.navigate(['/public/catalogo']);
              break;
          }
        } else {
          mostrarToast('Error inesperado. Intenta nuevamente.', 'danger');
        }
      },
      error: (error) => {
        this.loading = false;

        if (error.status === 401) {
          mostrarToast('Credenciales inválidas. Verifica tus datos.', 'danger');
        } else if (error.status === 0) {
          mostrarToast('No se pudo conectar con el servidor.', 'danger');
        } else {
          mostrarToast('Ocurrió un error al iniciar sesión.', 'danger');
        }
      }
    });
  }
}
