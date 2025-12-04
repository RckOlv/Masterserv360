import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
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

  // MENTOR: Variables para visibilidad de contraseñas
  public hideActual = true;
  public hideNueva = true;
  public hideRepetir = true;

  // MENTOR: Lista de Países
  public paises = [
    { nombre: 'Argentina', codigo: '+54', bandera: '🇦🇷' },
    { nombre: 'Brasil', codigo: '+55', bandera: '🇧🇷' },
    { nombre: 'Paraguay', codigo: '+595', bandera: '🇵🇾' },
    { nombre: 'Uruguay', codigo: '+598', bandera: '🇺🇾' },
    { nombre: 'Chile', codigo: '+56', bandera: '🇨🇱' },
    { nombre: 'Bolivia', codigo: '+591', bandera: '🇧🇴' }
  ];

  constructor() {
    const textPattern = /^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/;

    // Formulario Datos Personales
    this.perfilForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.minLength(2), Validators.pattern(textPattern)]],
      apellido: ['', [Validators.required, Validators.minLength(2), Validators.pattern(textPattern)]],
      email: [{ value: '', disabled: true }], 
      tipoDocumentoId: [null, Validators.required],
      documento: ['', [Validators.required, Validators.pattern(/^[0-9]{7,11}$/)]],
      
      // Teléfono dividido
      codigoPais: ['+54'],
      telefono: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(15)]]
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

  // MENTOR: Métodos toggle
  toggleActual() { this.hideActual = !this.hideActual; }
  toggleNueva() { this.hideNueva = !this.hideNueva; }
  toggleRepetir() { this.hideRepetir = !this.hideRepetir; }

  passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const pass = control.get('passwordNueva')?.value;
    const confirm = control.get('passwordRepetir')?.value;
    return pass === confirm ? null : { mismatch: true };
  }
  
  // Validadores de bloqueo de teclas
  validarInputNumerico(event: any): void {
      const input = event.target;
      input.value = input.value.replace(/[^0-9]/g, '');
      const controlName = input.getAttribute('formControlName');
      if (controlName) this.perfilForm.get(controlName)?.setValue(input.value);
  }

  validarInputTexto(event: any): void {
      const input = event.target;
      input.value = input.value.replace(/[^a-zA-ZáéíóúÁÉÍÓÚñÑ\s]/g, '');
      const controlName = input.getAttribute('formControlName');
      if (controlName) this.perfilForm.get(controlName)?.setValue(input.value);
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
        
        // MENTOR: Separar teléfono
        let telefonoFull = data.telefono || '';
        let codigo = '+54';
        let numero = telefonoFull;

        if (telefonoFull.startsWith('+549')) { 
            codigo = '+54';
            numero = telefonoFull.substring(4); 
        } else {
            const pais = this.paises.find(p => telefonoFull.startsWith(p.codigo));
            if (pais) {
                codigo = pais.codigo;
                numero = telefonoFull.substring(codigo.length);
            }
        }

        this.perfilForm.patchValue({
          nombre: data.nombre,
          apellido: data.apellido,
          email: data.email,
          tipoDocumentoId: data.tipoDocumentoId, 
          documento: data.documento,
          codigoPais: codigo,
          telefono: numero
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
    this.perfilForm.markAllAsTouched();
    if (this.perfilForm.invalid) {
      mostrarToast('Revise los datos personales.', 'warning');
      return;
    }

    this.isSubmitting = true;
    
    // Extraemos codigoPais para no mandarlo sucio, pero lo usamos para armar el teléfono
    const { codigoPais, ...restForm } = this.perfilForm.getRawValue();
    
    // Lógica de unión
    let telefonoFinal = '';
    let numeroLimpio = restForm.telefono ? restForm.telefono.trim() : '';
    
    if (numeroLimpio) {
        if (codigoPais === '+54' && !numeroLimpio.startsWith('9')) {
            telefonoFinal = `${codigoPais}9${numeroLimpio}`;
        } else {
            telefonoFinal = `${codigoPais}${numeroLimpio}`;
        }
    }

    const usuarioUpdate = { 
        ...this.usuarioActual, 
        ...restForm,
        telefono: telefonoFinal // Sobreescribimos con el formateado
    };

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
  
  get f() { return this.perfilForm.controls; }
}