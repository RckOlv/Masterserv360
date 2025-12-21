import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../service/auth.service'; // Ajusta la ruta si es necesario
import { mostrarToast } from '../../../utils/toast'; // Tu utilidad de alertas

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.css'
})
export class ForgotPasswordComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);

  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]]
  });

  isLoading = false;
  emailSent = false;

  onSubmit() {
    if (this.form.invalid) return;

    this.isLoading = true;
    const email = this.form.value.email!;

    this.authService.solicitarRecuperacion(email).subscribe({
      next: () => {
        this.isLoading = false;
        this.emailSent = true; // Muestra mensaje de éxito
        mostrarToast('Correo enviado. Revisa tu bandeja.', 'success');
      },
      error: (err) => {
        this.isLoading = false;
        // Mostramos el error que viene del backend o uno genérico
        mostrarToast(err.error?.message || 'Error al solicitar recuperación', 'danger');
      }
    });
  }
}