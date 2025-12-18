import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../service/auth.service';
import { mostrarToast } from '../../utils/toast';

@Component({
  selector: 'app-change-password-force',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './change-password-force.html',
  styleUrls: ['./change-password-force.css']
})
export default class ChangePasswordForceComponent {

  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  public form: FormGroup;
  public isSubmitting = false;

  constructor() {
    this.form = this.fb.group({
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.passwordsMatchValidator });
  }

  // Validador para que coincidan las contraseñas
  passwordsMatchValidator(group: FormGroup) {
    const pass = group.get('password')?.value;
    const confirm = group.get('confirmPassword')?.value;
    return pass === confirm ? null : { notMatching: true };
  }

  onSubmit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    const nuevaPass = this.form.get('password')?.value;

    // Llamamos al servicio que creamos antes
    this.authService.cambiarPasswordInicial(nuevaPass).subscribe({
      next: () => {
        mostrarToast('¡Contraseña actualizada! Bienvenido.', 'success');
        
        // Ahora sí, redirigimos a donde corresponda
        if (this.authService.hasRole('ROLE_ADMIN') || this.authService.hasRole('ROLE_VENDEDOR')) {
            this.router.navigate(['/pos/dashboard']);
        } else {
            this.router.navigate(['/portal/catalogo']);
        }
      },
      error: (err: any) => { // <--- Agregamos ": any"
        console.error(err);
        mostrarToast('Error al actualizar contraseña.', 'danger');
        this.isSubmitting = false;
      }
    });
  }
}