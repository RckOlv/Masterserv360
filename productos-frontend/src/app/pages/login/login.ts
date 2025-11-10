import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http'; 

import { AuthService } from '../../service/auth.service';
import { LoginRequestDTO } from '../../models/login-request.model';
import { mostrarToast } from '../../utils/toast'; // (Importo tu toast para los errores)

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule, 
    HttpClientModule 
  ],
  templateUrl: './login.html',
  styleUrls: ['./login.css']
})
export default class LoginComponent {

  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  public loginForm: FormGroup;
  public errorMessage: string | null = null;
  public isSubmitting = false; // (Buena práctica añadir esto)
  
  constructor() {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  onSubmit(): void {
    this.loginForm.markAllAsTouched();
    if (this.loginForm.invalid || this.isSubmitting) {
      return;
    }

    this.isSubmitting = true; // Bloquea el botón
    this.errorMessage = null;
    const credentials = this.loginForm.value as LoginRequestDTO;

    this.authService.login(credentials).subscribe({
      
      // --- ¡AQUÍ ESTÁ LA CORRECCIÓN! ---
      next: (response) => {
        // ¡Éxito! El AuthService ya guardó el token.
        // Ahora, leemos el token guardado para ver qué rol tiene el usuario.
        
        if (this.authService.hasRole('ROLE_ADMIN') || this.authService.hasRole('ROLE_VENDEDOR')) {
          
          // Si es Admin/Vendedor, lo mandamos al dashboard del POS
          this.router.navigate(['/pos/dashboard']); 

        } else if (this.authService.hasRole('ROLE_CLIENTE')) {
          
          // Si es Cliente, lo mandamos al catálogo del Portal
          this.router.navigate(['/portal/catalogo']);

        } else {
          // Si es un rol desconocido o sin rol, lo mandamos al login con error
          this.isSubmitting = false;
          this.errorMessage = 'Rol de usuario no reconocido.';
          this.authService.logout(); // Limpiamos el token inválido
        }
      },
      // --- FIN DE LA CORRECCIÓN ---
      
      error: (err) => {
        console.error('Error en el login:', err);
        this.errorMessage = 'Credenciales inválidas. Por favor, intente de nuevo.';
        this.isSubmitting = false; // Desbloquea el botón
        mostrarToast(this.errorMessage, 'danger');
      }
    });
  }

  // --- Helpers para mostrar errores en el template ---
  get email() { return this.loginForm.get('email'); }
  get password() { return this.loginForm.get('password'); }
}