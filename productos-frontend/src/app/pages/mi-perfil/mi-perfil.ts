import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { ClienteService } from '../../service/cliente.service';
import { TipoDocumentoService } from '../../service/tipo-documento.service';
import { ClientePerfilUpdateDTO } from '../../models/cliente-perfil-update.model';
import { TipoDocumentoDTO } from '../../models/tipo-documento.model';
import { mostrarToast } from '../../utils/toast';
import { HttpErrorResponse } from '@angular/common/http';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

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
  
  public maxDocumentoLength: number = 20;

  public hideActual = true;
  public hideNueva = true;
  public hideRepetir = true;

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
      nombre: ['', [Validators.required, Validators.maxLength(100), Validators.pattern(textPattern)]],
      apellido: ['', [Validators.required, Validators.maxLength(100), Validators.pattern(textPattern)]],
      
      codigoPais: ['+54'],
      telefono: ['', [Validators.maxLength(15), Validators.minLength(8)]],
      
      tipoDocumentoId: [null, [Validators.required]],
      documento: ['', [Validators.required]]
    });
    
    this.passwordForm = this.fb.group({
      passwordActual: ['', Validators.required],
      passwordNueva: ['', [Validators.required, Validators.minLength(6)]],
      passwordRepetir: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });
  }

  ngOnInit(): void {
    this.loadTiposDocumento();
    this.loadPerfil();
    this.setupDocumentValidation();
  }

  toggleActual() { this.hideActual = !this.hideActual; }
  toggleNueva() { this.hideNueva = !this.hideNueva; }
  toggleRepetir() { this.hideRepetir = !this.hideRepetir; }

  passwordMatchValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
    const pass = control.get('passwordNueva')?.value;
    const confirm = control.get('passwordRepetir')?.value;
    if (!pass && !confirm) return null;
    return pass === confirm ? null : { mismatch: true };
  }
  
  validarInputNumerico(event: any): void {
      const input = event.target;
      input.value = input.value.replace(/[^0-9]/g, '');
      this.actualizarControl(input, this.perfilForm);
  }

  validarInputTexto(event: any): void {
      const input = event.target;
      input.value = input.value.replace(/[^a-zA-ZáéíóúÁÉÍÓÚñÑ\s]/g, '');
      this.actualizarControl(input, this.perfilForm);
  }
  
  private actualizarControl(input: any, form: FormGroup) {
      const controlName = input.getAttribute('formControlName');
      if (controlName && form.get(controlName)) {
          form.get(controlName)?.setValue(input.value);
      }
  }

  loadTiposDocumento(): void {
    this.tipoDocumentoService.listarTiposDocumento().subscribe({
      next: (data) => this.tiposDocumento = data,
      error: () => mostrarToast('Error al cargar tipos de documento', 'danger')
    });
  }
  
  private setupDocumentValidation(): void {
    const tipoDocControl = this.perfilForm.get('tipoDocumentoId');
    const docControl = this.perfilForm.get('documento');

    if (!tipoDocControl || !docControl) return;

    tipoDocControl.valueChanges.pipe(
      debounceTime(100),
      distinctUntilChanged()
    ).subscribe(tipoId => {
      docControl.clearValidators();
      let validadores = [Validators.required];
      const idNumerico = +tipoId;
      const tipoSeleccionado = this.tiposDocumento.find(t => t.id === idNumerico);
      const nombreTipo = tipoSeleccionado ? tipoSeleccionado.nombreCorto.toUpperCase() : '';
      
      switch (nombreTipo) {
        case 'DNI':
          this.maxDocumentoLength = 8;
          validadores.push(Validators.minLength(7), Validators.maxLength(8));
          break;  
        case 'CUIT':
          this.maxDocumentoLength = 11;
          validadores.push(Validators.minLength(11), Validators.maxLength(11));
          break;
        case 'PAS':
          this.maxDocumentoLength = 20; 
          validadores.push(Validators.minLength(6));
          break;
        default:
          this.maxDocumentoLength = 20;
      }
      docControl.setValidators(validadores);
      docControl.updateValueAndValidity();
    });
  }

  loadPerfil(): void {
    this.isLoading = true;
    this.perfilForm.disable(); 
    this.clienteService.getMiPerfil().subscribe({
      next: (data) => {
        // --- MENTOR: LÓGICA INTELIGENTE DE SEPARACIÓN (+549) ---
        let telefonoFull = data.telefono || '';
        let codigo = '+54';
        let numero = telefonoFull;

        if (telefonoFull.startsWith('+549')) { 
            codigo = '+54';
            numero = telefonoFull.substring(4); // Quitamos +549
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
          telefono: numero,
          codigoPais: codigo, 
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
    this.perfilForm.markAllAsTouched();
    if (this.perfilForm.invalid) {
      mostrarToast('Revise los datos personales.', 'warning');
      return;
    }

    this.isSubmitting = true;
    const formValue = this.perfilForm.value;
    
    // 1. Lógica de unión de teléfono
    let telefonoFinal = '';
    let numeroLimpio = formValue.telefono ? formValue.telefono.trim() : '';
    let codigoPais = formValue.codigoPais;

    if (numeroLimpio) {
        if (codigoPais === '+54' && !numeroLimpio.startsWith('9')) {
            telefonoFinal = `${codigoPais}9${numeroLimpio}`;
        } else {
            telefonoFinal = `${codigoPais}${numeroLimpio}`;
        }
    }

    // 2. CREAMOS EL OBJETO LIMPIO (Solución al Error 500)
    // Mapeamos campo a campo para evitar enviar 'codigoPais'
    const updateDTO: ClientePerfilUpdateDTO = {
        nombre: formValue.nombre,
        apellido: formValue.apellido,
        telefono: telefonoFinal, // Usamos el procesado
        tipoDocumentoId: formValue.tipoDocumentoId,
        documento: formValue.documento
    };

    this.clienteService.updateMiPerfil(updateDTO).subscribe({
      next: () => {
        mostrarToast('¡Perfil actualizado!', 'success');
        this.isSubmitting = false;
        // Opcional: recargar los datos para asegurar sincronización
        this.loadPerfil(); 
      },
      error: (err) => {
        this.isSubmitting = false;
        console.error(err);
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
  
  get f() { return this.perfilForm.controls; }
  
  get selectedTipoDocNombre(): string {
    const tipoId = this.perfilForm.get('tipoDocumentoId')?.value;
    if (!tipoId || !this.tiposDocumento.length) return '';
    const tipo = this.tiposDocumento.find(t => t.id === +tipoId);
    return tipo ? tipo.nombreCorto.toUpperCase() : '';
  }
}