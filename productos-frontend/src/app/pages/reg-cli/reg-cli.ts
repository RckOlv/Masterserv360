import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';

import { AuthService } from '../../service/auth.service';
import { TipoDocumentoService } from '../../service/tipo-documento.service';
import { RegisterRequestDTO } from '../../models/register-request.model';
import { TipoDocumentoDTO } from '../../models/tipo-documento.model';
import { mostrarToast } from '../../utils/toast';

@Component({
  selector: 'app-reg-cli',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './reg-cli.html',
  styleUrls: ['./reg-cli.css']
})
export default class RegCliComponent implements OnInit {

  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private tipoDocumentoService = inject(TipoDocumentoService);
  private router = inject(Router);

  public registerForm: FormGroup;
  public errorMessage: string | null = null;
  public tiposDocumento: TipoDocumentoDTO[] = [];

  constructor() {
    // Definición del formulario con TODOS los campos como OBLIGATORIOS
    this.registerForm = this.fb.group({
      nombre: ['', Validators.required],
      apellido: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      tipoDocumentoId: [null, Validators.required], // OBLIGATORIO
      documento: ['', Validators.required],           // OBLIGATORIO
      telefono: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    // Cargar los tipos de documento para el dropdown
    this.tipoDocumentoService.listarTiposDocumento().subscribe({
      next: (data) => this.tiposDocumento = data,
      error: (err: any) => {
        console.error('Error cargando tipos de documento', err);
        this.errorMessage = "Error al cargar tipos de documento. Recargue la página.";
      }
    });
  }

  onSubmit(): void {
    this.registerForm.markAllAsTouched();
    
    if (this.registerForm.invalid) {
      mostrarToast("Por favor, complete todos los campos obligatorios.", "warning");
      return;
    }

    this.errorMessage = null;
    const request = this.registerForm.value as RegisterRequestDTO;

    this.authService.register(request).subscribe({
      next: () => {
        mostrarToast('¡Registro exitoso! Por favor, inicie sesión.', 'success'); 
        this.router.navigate(['/login']);
      },
      error: (err: any) => {
        console.error('Error en el registro:', err);
        this.errorMessage = err.error?.message || 'Error al registrar el usuario.';
        if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
      }
    });
  }

  // Helper para validación en template
  get f() { return this.registerForm.controls; }
}