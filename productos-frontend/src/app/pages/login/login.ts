import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http'; 

import { AuthService } from '../../service/auth.service';
import { LoginRequestDTO } from '../../models/login-request.model';
import { mostrarToast } from '../../utils/toast';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ CommonModule, RouterLink, ReactiveFormsModule, HttpClientModule ],
  templateUrl: './login.html',
  styleUrls: ['./login.css']
})
export default class LoginComponent {

  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  public loginForm: FormGroup;
  public errorMessage: string | null = null;
  public isSubmitting = false;
  
  constructor() {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  onSubmit(): void {
    this.loginForm.markAllAsTouched();
    if (this.loginForm.invalid || this.isSubmitting) return;

    this.isSubmitting = true; 
    this.errorMessage = null;
    const credentials = this.loginForm.value as LoginRequestDTO;

    this.authService.login(credentials).subscribe({
      next: (response) => {
        
        // --- 1. PRIMERO: VERIFICAR CAMBIO OBLIGATORIO ---
        // Si el backend dice true, mandamos a la "cárcel" y detenemos todo.
        if (response.debeCambiarPassword) {
            this.router.navigate(['/auth/cambiar-password-force']);
            return; // <--- ¡MUY IMPORTANTE! Evita que siga ejecutando el código de abajo
        }

        // --- 2. SI NO HAY BANDERA, FLUJO NORMAL ---
        if (this.authService.hasRole('ROLE_ADMIN') || this.authService.hasRole('ROLE_VENDEDOR')) {
          // Admin/Vendedor -> Directo al Dashboard
          this.router.navigate(['/pos/dashboard']); 
        } else {
          // Cliente -> Al Catálogo (Portal)
          this.router.navigate(['/catalogo']);
        }
      },
      error: (err) => {
        console.error('Error en el login:', err);
        // Si es 401, suele ser credenciales. Si es otro, error genérico.
        this.errorMessage = 'Credenciales inválidas. Por favor, intente de nuevo.';
        this.isSubmitting = false; 
        mostrarToast(this.errorMessage, 'danger');
      }
    });
  }

  get email() { return this.loginForm.get('email'); }
  get password() { return this.loginForm.get('password'); }
}