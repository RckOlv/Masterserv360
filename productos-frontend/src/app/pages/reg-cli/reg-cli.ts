import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';

// 1. Importamos el DTO y el Servicio correctos
import { AuthService } from '../../service/auth.service';
import { RegisterRequestDTO } from '../../models/register-request.model';

@Component({
  selector: 'app-registro-cliente', // Tu selector
  standalone: true,
  // 2. Importamos ReactiveFormsModule
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule, // <-- CAMBIO
    HttpClientModule
  ],
  templateUrl: './reg-cli.html',
  styleUrls: ['./reg-cli.css']
})
export default class RegistroClienteComponent {

  // 3. Inyectamos AuthService y FormBuilder
  private fb = inject(FormBuilder);
  private authService = inject(AuthService); // <-- CAMBIO
  private router = inject(Router);

  public registerForm: FormGroup;
  public errorMessage: string | null = null;
  
  constructor() {
    // 4. Creamos el Formulario Reactivo
    this.registerForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
      apellido: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      // Los campos opcionales del DTO
      tipoDocumentoId: [null],
      documento: ['', [Validators.maxLength(30)]],
      telefono: ['', [Validators.maxLength(20)]]
    });
  }

  onSubmit(): void {
    this.registerForm.markAllAsTouched();
    
    if (this.registerForm.invalid) {
      return;
    }

    this.errorMessage = null;

    // 5. Mapeamos el formulario al DTO
    const request = this.registerForm.value as RegisterRequestDTO;

    // 6. Llamamos al servicio correcto
    this.authService.register(request).subscribe({
      next: () => {
        alert('Registro exitoso. Por favor, inicie sesión.'); // Usamos 'alert' como tenías
        this.router.navigate(['/login']);
      },
      error: (err) => {
        console.error('Error en el registro:', err);
        // El GlobalExceptionHandler del backend nos da el mensaje
        this.errorMessage = err.error?.message || 'Error al registrar el usuario.';
      }
    });
  }

  // --- Helpers para mostrar errores en el template ---
  get f() { return this.registerForm.controls; }
}