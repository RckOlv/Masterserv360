import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { ClienteService } from '../../service/cliente.service';
import { TipoDocumentoService } from '../../service/tipo-documento.service';
import { ClientePerfilUpdateDTO } from '../../models/cliente-perfil-update.model';
import { TipoDocumentoDTO } from '../../models/tipo-documento.model';
import { mostrarToast } from '../../utils/toast';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-mi-perfil',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule], 
  templateUrl: './mi-perfil.html',
  styleUrl: './mi-perfil.css'
})
export default class MiPerfilComponent implements OnInit {

  private fb = inject(FormBuilder);
  private clienteService = inject(ClienteService);
  private tipoDocumentoService = inject(TipoDocumentoService);

  public perfilForm: FormGroup;
  public passwordForm: FormGroup; 

  public tiposDocumento: TipoDocumentoDTO[] = [];
  public isLoading = true;
  public isSubmitting = false;
  public isSubmittingPass = false;

  constructor() {
    // Formulario Datos
    this.perfilForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.maxLength(100)]],
      apellido: ['', [Validators.required, Validators.maxLength(100)]],
      telefono: ['', [Validators.maxLength(20)]],
      tipoDocumentoId: [null, [Validators.required]],
      documento: ['', [Validators.required, Validators.maxLength(20)]]
    });
    
    // Formulario Password
    this.passwordForm = this.fb.group({
      passwordActual: ['', Validators.required],
      passwordNueva: ['', [Validators.required, Validators.minLength(6)]],
      passwordRepetir: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });
  }

  ngOnInit(): void {
    this.loadTiposDocumento();
    this.loadPerfil();
  }

  passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const passwordNueva = control.get('passwordNueva')?.value;
    const passwordRepetir = control.get('passwordRepetir')?.value;
    return passwordNueva === passwordRepetir ? null : { mismatch: true };
  }

  loadTiposDocumento(): void {
    this.tipoDocumentoService.listarTiposDocumento().subscribe({
      next: (data) => this.tiposDocumento = data,
      error: () => mostrarToast('Error al cargar tipos de documento', 'danger')
    });
  }

  loadPerfil(): void {
    this.isLoading = true;
    this.perfilForm.disable(); 
    this.clienteService.getMiPerfil().subscribe({
      next: (data) => {
        this.perfilForm.patchValue({
          nombre: data.nombre,
          apellido: data.apellido,
          telefono: data.telefono,
          tipoDocumentoId: data.tipoDocumentoId,
          documento: data.documento
        });
        this.perfilForm.enable();
        this.isLoading = false;
      },
      error: () => {
        mostrarToast('Error al cargar el perfil', 'danger');
        this.isLoading = false;
      }
    });
  }

  onSubmit(): void {
    if (this.perfilForm.invalid) {
      this.perfilForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    const updateDTO: ClientePerfilUpdateDTO = this.perfilForm.value;

    this.clienteService.updateMiPerfil(updateDTO).subscribe({
      next: () => {
        mostrarToast('¡Perfil actualizado!', 'success');
        this.isSubmitting = false;
      },
      error: () => {
        this.isSubmitting = false;
        mostrarToast('Error al actualizar perfil', 'danger');
      }
    });
  }

  onChangePassword(): void {
    if (this.passwordForm.invalid) return;

    this.isSubmittingPass = true;
    const { passwordActual, passwordNueva } = this.passwordForm.value;

    this.clienteService.cambiarPassword({ passwordActual, passwordNueva }).subscribe({
      next: () => {
        mostrarToast('Contraseña cambiada exitosamente.', 'success');
        this.passwordForm.reset();
        this.isSubmittingPass = false;
      },
      error: (err: HttpErrorResponse) => {
        this.isSubmittingPass = false;
        const msg = err.error?.message || 'Error al cambiar contraseña.'; 
        mostrarToast(msg, 'danger');
      }
    });
  }
}