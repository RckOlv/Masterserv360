import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms'; // Import Reactive
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';

// 1. Importar AuthService y el DTO correcto
import { AuthService } from '../../../service/auth.service'; // Ajusta la ruta si es necesario
import { RegisterRequestDTO } from '../../../models/register-request.model';
// import { RolService } from '../../../service/rol.service'; // Ya no lo usamos aquí
// import { RolDTO } from '../../../models/rol.model'; // Ya no lo usamos aquí

@Component({
  standalone: true,
  selector: 'app-registro-admin', // Tu selector
  templateUrl: './registro.html',
  styleUrls: ['./registro.css'],
  // 2. Usar ReactiveFormsModule y RouterLink
  imports: [
    CommonModule,
    ReactiveFormsModule, // <-- CAMBIO
    HttpClientModule,
    RouterLink // Para el botón de cancelar/volver
  ]
})
// 3. Renombra si quieres, o déjalo como RegistroAdminComponent
export default class RegistroAdminComponent implements OnInit {

  // 4. Inyectar FormBuilder y AuthService
  private fb = inject(FormBuilder);
  private authService = inject(AuthService); // <-- CAMBIO
  private router = inject(Router);

  public registerForm: FormGroup;
  public errorMessage: string | null = null;
  // public roles: RolDTO[] = []; // Comentado por ahora

  constructor() {
    // 5. Crear el Formulario Reactivo
    this.registerForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
      apellido: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      tipoDocumentoId: [null], // Mantener opcionales
      documento: ['', [Validators.maxLength(30)]],
      telefono: ['', [Validators.maxLength(20)]]
      // rolId: [null, Validators.required] // Comentado por ahora
    });
  }

  ngOnInit(): void {
    // this.cargarRoles(); // Comentado por ahora
  }

  /* // Comentado por ahora
  cargarRoles(): void {
    this.rolService.listarRoles().subscribe({
      next: (roles) => this.roles = roles,
      error: () => alert('Error al cargar los roles') // Mejorar manejo de errores
    });
  }
  */

  onSubmit(): void {
    this.registerForm.markAllAsTouched();

    if (this.registerForm.invalid) {
      return;
    }

    this.errorMessage = null;

    // 6. Mapear al DTO (sin el rol por ahora)
    const request = this.registerForm.value as RegisterRequestDTO;

    // 7. Llamar a AuthService.register
    this.authService.register(request).subscribe({
      next: () => {
        alert('Usuario creado con éxito'); // O usar un toast
        this.router.navigate(['/usuarios']); // O a donde redirija el admin
      },
      error: (err) => {
        console.error('Error al crear usuario:', err);
        this.errorMessage = err.error?.message || 'Error al crear el usuario.';
      }
    });
  }

  // Helpers para el template
  get f() { return this.registerForm.controls; }
}