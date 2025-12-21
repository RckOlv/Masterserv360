import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../service/auth.service';
import { mostrarToast } from '../../../utils/toast';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.html',
  styleUrl: './reset-password.css' // Asegúrate que exista el css o borra esta linea
})
export class ResetPasswordComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private authService = inject(AuthService);

  token: string | null = null;
  isLoading = false;

  form = this.fb.group({
    password: ['', [Validators.required, Validators.minLength(6)]],
    confirmPassword: ['', [Validators.required]]
  });

  ngOnInit() {
    // Capturar el token de la URL: /reset-password?token=XYZ123
    this.token = this.route.snapshot.queryParamMap.get('token');
    
    if (!this.token) {
      mostrarToast('Enlace inválido. No hay token.', 'danger');
      this.router.navigate(['/login']);
    }
  }

  onSubmit() {
    if (this.form.invalid || !this.token) return;

    const { password, confirmPassword } = this.form.value;

    if (password !== confirmPassword) {
      mostrarToast('Las contraseñas no coinciden', 'warning');
      return;
    }

    this.isLoading = true;
    this.authService.restablecerContrasena(this.token, password!).subscribe({
      next: () => {
        mostrarToast('¡Contraseña actualizada! Inicia sesión.', 'success');
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.isLoading = false;
        mostrarToast(err.error?.message || 'Error al restablecer contraseña', 'danger');
      }
    });
  }
}