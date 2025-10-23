import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common'; // Para usar *ngIf
import { HttpClientModule } from '@angular/common/http'; // A veces necesario si es standalone

import { AuthService } from '../../service/auth.service'; // Ajusta la ruta si es necesario
import { LoginRequestDTO } from '../../models/login-request.model';

@Component({
  selector: 'app-login',
  standalone: true,
  // Importamos todo lo que el template y el componente necesitan
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule, 
    HttpClientModule 
  ],
  templateUrl: './login.html',
  styleUrls: ['./login.css']
})
export default class LoginComponent { // Asumo 'export default' por tu estructura

  // Inyección de dependencias moderna (usando inject())
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  // El formulario reactivo
  public loginForm: FormGroup;
  public errorMessage: string | null = null;
  
  constructor() {
    this.loginForm = this.fb.group({
      // Definimos los campos y sus validadores
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  /**
   * Método que se llama al enviar el formulario
   */
  onSubmit(): void {
    // Marcamos todos los campos como "tocados" para mostrar errores
    this.loginForm.markAllAsTouched();
    
    // Si el formulario no es válido, detenemos
    if (this.loginForm.invalid) {
      return;
    }

    // Limpiamos errores previos
    this.errorMessage = null;

    // Obtenemos los valores del formulario
    const credentials = this.loginForm.value as LoginRequestDTO;

    // Llamamos a nuestro AuthService
    this.authService.login(credentials).subscribe({
      next: (response) => {
        // ¡Éxito! El AuthService ya guardó el token.
        // Redirigimos al dashboard (o a la página principal del admin)
        this.router.navigate(['/dashboard']); 
      },
      error: (err) => {
        // El GlobalExceptionHandler del backend nos dará un 401
        console.error('Error en el login:', err);
        this.errorMessage = 'Credenciales inválidas. Por favor, intente de nuevo.';
      }
    });
  }

  // --- Helpers para mostrar errores en el template ---
  get email() { return this.loginForm.get('email'); }
  get password() { return this.loginForm.get('password'); }
}