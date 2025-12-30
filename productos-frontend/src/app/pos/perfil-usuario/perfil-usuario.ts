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

  public hideActual = true;
  public hideNueva = true;
  public hideRepetir = true;

  // 🟢 Lista Manual "Hardcoded"
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

    this.perfilForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.minLength(2), Validators.pattern(textPattern)]],
      apellido: ['', [Validators.required, Validators.minLength(2), Validators.pattern(textPattern)]],
      email: [{ value: '', disabled: true }], 
      tipoDocumentoId: [null, Validators.required],
      documento: ['', [Validators.required, Validators.pattern(/^[0-9]{7,11}$/)]],
      
      // 🟢 Campos Separados para Teléfono
      codigoPais: ['+54', Validators.required],
      telefono: ['', [Validators.required, Validators.pattern(/^[0-9]{8,15}$/)]]
    });

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

  toggleActual() { this.hideActual = !this.hideActual; }
  toggleNueva() { this.hideNueva = !this.hideNueva; }
  toggleRepetir() { this.hideRepetir = !this.hideRepetir; }

  passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const pass = control.get('passwordNueva')?.value;
    const confirm = control.get('passwordRepetir')?.value;
    return pass === confirm ? null : { mismatch: true };
  }

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
        
        // 🟢 LÓGICA DE PARSEO INTELIGENTE (Detectar +549)
        let telefonoFull = data.telefono || '';
        let codigo = '+54'; // Default
        let numero = '';

        if (telefonoFull) {
            // Caso especial Argentina Móvil (+549...)
            if (telefonoFull.startsWith('+549')) {
                codigo = '+54';
                numero = telefonoFull.substring(4); // Sacamos '+549'
            } else {
                // Buscamos coincidencia estándar
                const paisMatch = this.paises.find(p => telefonoFull.startsWith(p.codigo));
                if (paisMatch) {
                    codigo = paisMatch.codigo;
                    numero = telefonoFull.substring(codigo.length);
                } else {
                    // Si no coincide con nada conocido, dejamos default y todo el numero
                    numero = telefonoFull; 
                }
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
    if (this.perfilForm.invalid) {
      mostrarToast('Revise los datos personales.', 'warning');
      this.perfilForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    const f = this.perfilForm.value;

    // 🟢 LÓGICA DE UNIÓN Y "EL 9 MÁGICO"
    let numeroLimpio = f.telefono ? f.telefono.trim() : '';
    let telefonoFinal = '';

    if (f.codigoPais === '+54' && !numeroLimpio.startsWith('9')) {
        // Es Argentina y falta el 9 -> Agregamos 9
        telefonoFinal = `${f.codigoPais}9${numeroLimpio}`;
    } else {
        // Resto de casos (o si ya puso el 9)
        telefonoFinal = `${f.codigoPais}${numeroLimpio}`;
    }

    const usuarioUpdate = { 
        ...this.usuarioActual, 
        ...f,
        telefono: telefonoFinal 
    };

    this.usuarioService.actualizarMiPerfil(usuarioUpdate).subscribe({
      next: (data) => {
        mostrarToast('Perfil actualizado correctamente.', 'success');
        this.usuarioActual = data;
        this.isSubmitting = false;
        // Recargar para verificar que se guardó y parsea bien
        this.loadMiPerfil();
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
        const msg = err.error?.message || 'Error al cambiar contraseña.';
        mostrarToast(msg, 'danger');
        this.isSubmittingPass = false;
      }
    });
  }
}