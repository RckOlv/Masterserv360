import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { UsuarioService } from '../../service/usuario.service';
import { TipoDocumentoService } from '../../service/tipo-documento.service';
import { TipoDocumentoDTO } from '../../models/tipo-documento.model';
import { UsuarioDTO } from '../../models/usuario.model';
import { mostrarToast } from '../../utils/toast';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-perfil-usuario',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './perfil-usuario.html',
  styleUrls: ['./perfil-usuario.css']
})
export default class PerfilUsuarioComponent implements OnInit {

  private fb = inject(FormBuilder);
  private usuarioService = inject(UsuarioService);
  private tipoDocumentoService = inject(TipoDocumentoService);

  public perfilForm: FormGroup;
  public passwordForm: FormGroup;

  public tiposDocumento: TipoDocumentoDTO[] = [];
  public isLoading = true;
  public isSubmitting = false;
  public isSubmittingPass = false;
  public usuarioActual: UsuarioDTO | null = null;

  constructor() {
    // Formulario Datos Personales
    this.perfilForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.minLength(2)]],
      apellido: ['', [Validators.required, Validators.minLength(2)]],
      email: [{ value: '', disabled: true }], // El email no se edita por seguridad
      tipoDocumentoId: [null],
      documento: [''],
      telefono: ['']
    });

    // Formulario Cambio de Contraseña
    this.passwordForm = this.fb.group({
      passwordActual: ['', Validators.required],
      passwordNueva: ['', [Validators.required, Validators.minLength(6)]],
      passwordRepetir: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });
  }

  ngOnInit(): void {
    this.loadTiposDocumento();
    this.loadMiPerfil();
  }

  // Validador para confirmar password
  passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const pass = control.get('passwordNueva')?.value;
    const confirm = control.get('passwordRepetir')?.value;
    return pass === confirm ? null : { mismatch: true };
  }

  loadTiposDocumento(): void {
    this.tipoDocumentoService.listarTiposDocumento().subscribe({
      next: (data) => this.tiposDocumento = data
    });
  }

  loadMiPerfil(): void {
    this.isLoading = true;
    this.usuarioService.getMiPerfil().subscribe({
      next: (data) => {
        this.usuarioActual = data;
        
        // Mapeamos los datos al formulario
        this.perfilForm.patchValue({
          nombre: data.nombre,
          apellido: data.apellido,
          email: data.email,
          tipoDocumentoId: data.tipoDocumentoId, // Asegúrate que tu UsuarioDTO tenga este campo
          documento: data.documento,
          telefono: data.telefono
        });
        this.isLoading = false;
      },
      error: (err) => {
        mostrarToast('Error al cargar perfil.', 'danger');
        this.isLoading = false;
      }
    });
  }

  onSubmitPerfil(): void {
    if (this.perfilForm.invalid) {
      this.perfilForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    // Combinamos los datos del form con el objeto original para no perder IDs
    const usuarioUpdate = { ...this.usuarioActual, ...this.perfilForm.getRawValue() };

    this.usuarioService.actualizarMiPerfil(usuarioUpdate).subscribe({
      next: (data) => {
        mostrarToast('Perfil actualizado correctamente.', 'success');
        this.usuarioActual = data;
        this.isSubmitting = false;
      },
      error: (err: HttpErrorResponse) => {
        const msg = err.error?.message || 'Error al actualizar perfil.';
        mostrarToast(msg, 'danger');
        this.isSubmitting = false;
      }
    });
  }

  onSubmitPassword(): void {
    if (this.passwordForm.invalid) return;

    this.isSubmittingPass = true;
    const { passwordActual, passwordNueva } = this.passwordForm.value;

    this.usuarioService.cambiarPassword({ passwordActual, passwordNueva }).subscribe({
      next: () => {
        mostrarToast('Contraseña modificada. Inicia sesión nuevamente.', 'success');
        this.passwordForm.reset();
        this.isSubmittingPass = false;
      },
      error: (err: HttpErrorResponse) => {
        const msg = err.error?.message || 'Error al cambiar contraseña. Verifica tu clave actual.';
        mostrarToast(msg, 'danger');
        this.isSubmittingPass = false;
      }
    });
  }
}