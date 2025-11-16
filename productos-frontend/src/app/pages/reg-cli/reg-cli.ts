import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';

// Imports de RxJS para las validaciones dinámicas
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

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
  public isLoading = false;

  constructor() {
    this.registerForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.maxLength(50)]],
      apellido: ['', [Validators.required, Validators.maxLength(50)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
      password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]],
      tipoDocumentoId: [null, Validators.required],
      documento: ['', [Validators.required]], 
      telefono: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(15)]]
    });
  }

  ngOnInit(): void {
    this.cargarTiposDocumento();
  }

  cargarTiposDocumento(): void {
    this.tipoDocumentoService.listarTiposDocumento().subscribe({
      next: (data) => {
        this.tiposDocumento = data;
        this.setupDocumentValidation();
      },
      error: (err: any) => {
        console.error('Error cargando tipos de documento', err);
        this.errorMessage = "Error al cargar tipos de documento. Recargue la página.";
      }
    });
  }

  private setupDocumentValidation(): void {
    const tipoDocControl = this.registerForm.get('tipoDocumentoId');
    const docControl = this.registerForm.get('documento');

    if (!tipoDocControl || !docControl) return;

    tipoDocControl.valueChanges.pipe(
      debounceTime(100),
      distinctUntilChanged()
    ).subscribe(tipoId => {
      
      console.log("ID seleccionado (string):", tipoId);
      
      docControl.clearValidators();
      docControl.setValidators([Validators.required]);

      const idNumerico = +tipoId;
      const tipoSeleccionado = this.tiposDocumento.find(t => t.id === idNumerico);
      // Mentor: Usamos el nombreCorto (ej. "DNI", "CUIT", "PAS")
      const nombreTipo = tipoSeleccionado ? tipoSeleccionado.nombreCorto.toUpperCase() : ''; 

      console.log("Nombre corto encontrado:", nombreTipo);

      switch (nombreTipo) {
        case 'DNI':
          console.log("Aplicando reglas de DNI");
          docControl.addValidators([
            Validators.minLength(7),
            Validators.maxLength(8),
            Validators.pattern('^[0-9]*$') // Solo números
          ]);
          break; 
        
        case 'CUIT':
          console.log("Aplicando reglas de CUIT");
          docControl.addValidators([
            Validators.minLength(11),
            Validators.maxLength(11),
            Validators.pattern('^[0-9]*$') // Solo números
          ]);
          break;

        // --- Mentor: ¡VALIDACIÓN DE PASAPORTE AGREGADA! ---
        case 'PAS':
          console.log("Aplicando reglas de PAS (Pasaporte)");
          docControl.addValidators([
            Validators.minLength(6), // Longitud mínima común
            Validators.maxLength(20), // Longitud máxima flexible
            Validators.pattern('^[a-zA-Z0-9]*$') // Alfanumérico
          ]);
          break;
        // ----------------------------------------------------
      }

      docControl.updateValueAndValidity();
    });
  }

  onSubmit(): void {
    this.registerForm.markAllAsTouched();
    
    if (this.registerForm.invalid) {
      console.log("Formulario inválido:", this.registerForm.errors);
      console.log("Errores de 'documento':", this.registerForm.get('documento')?.errors);
      mostrarToast("Por favor, complete todos los campos obligatorios.", "warning");
      return;
    }

    this.isLoading = true; 
    this.errorMessage = null;
    const request = this.registerForm.value as RegisterRequestDTO;

    this.authService.register(request).subscribe({
      next: () => {
        this.isLoading = false;
        mostrarToast('¡Registro exitoso! Por favor, inicie sesión.', 'success'); 
        this.router.navigate(['/login']);
      },
      error: (err: any) => {
        this.isLoading = false;
        console.error('Error en el registro:', err);
        this.errorMessage = err.error?.message || 'Error al registrar el usuario.';
        if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
      }
    });
  }

  // Helper para validación en template HTML (el 'f')
  get f() { return this.registerForm.controls; }

  get selectedTipoDocNombre(): string {
    const tipoId = this.registerForm.get('tipoDocumentoId')?.value;
    if (!tipoId || !this.tiposDocumento.length) {
      return '';
    }
    const tipo = this.tiposDocumento.find(t => t.id === +tipoId);
    return tipo ? tipo.nombreCorto.toUpperCase() : '';
  }
  
}