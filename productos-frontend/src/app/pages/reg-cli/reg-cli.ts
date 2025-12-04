import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';

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
  
  public maxDocumentoLength: number = 20;

  public hidePassword = true;
  public hideConfirmPassword = true;

  public paises = [
    { nombre: 'Argentina', codigo: '+54', bandera: '🇦🇷' },
    { nombre: 'Brasil', codigo: '+55', bandera: '🇧🇷' },
    { nombre: 'Paraguay', codigo: '+595', bandera: '🇵🇾' },
    { nombre: 'Uruguay', codigo: '+598', bandera: '🇺🇾' },
    { nombre: 'Chile', codigo: '+56', bandera: '🇨🇱' },
    { nombre: 'Bolivia', codigo: '+591', bandera: '🇧🇴' }
  ];

  constructor() {
    const emailPattern = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    const textPattern = /^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/;

    this.registerForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.maxLength(50), Validators.pattern(textPattern)]],
      apellido: ['', [Validators.required, Validators.maxLength(50), Validators.pattern(textPattern)]],
      email: ['', [Validators.required, Validators.pattern(emailPattern), Validators.maxLength(100)]],
      
      password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]],
      confirmPassword: ['', [Validators.required]],
      
      tipoDocumentoId: [null, Validators.required],
      documento: ['', [Validators.required]], 
      
      codigoPais: ['+54', Validators.required],
      telefono: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(15)]]
    }, { validators: this.passwordMatchValidator });
  }

  ngOnInit(): void {
    this.cargarTiposDocumento();
    this.setupDocumentValidation();
  }

  togglePasswordVisibility() { this.hidePassword = !this.hidePassword; }
  toggleConfirmPasswordVisibility() { this.hideConfirmPassword = !this.hideConfirmPassword; }

  passwordMatchValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
      const password = control.get('password')?.value;
      const confirm = control.get('confirmPassword')?.value;
      if (!password && !confirm) return null;
      return password === confirm ? null : { mismatch: true };
  };

  validarInputNumerico(event: any): void {
      const input = event.target;
      input.value = input.value.replace(/[^0-9]/g, '');
      this.actualizarControl(input);
  }

  validarInputTexto(event: any): void {
      const input = event.target;
      input.value = input.value.replace(/[^a-zA-ZáéíóúÁÉÍÓÚñÑ\s]/g, '');
      this.actualizarControl(input);
  }

  private actualizarControl(input: any) {
      const controlName = input.getAttribute('formControlName');
      if (controlName && this.registerForm.get(controlName)) {
          this.registerForm.get(controlName)?.setValue(input.value);
      }
  }

  cargarTiposDocumento(): void {
    this.tipoDocumentoService.listarTiposDocumento().subscribe({
      next: (data) => { this.tiposDocumento = data; },
      error: (err: any) => {
        this.errorMessage = "Error al cargar tipos de documento.";
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
          validadores.push(Validators.minLength(6), Validators.maxLength(20));
          break;
        default:
          this.maxDocumentoLength = 20;
      }
      docControl.setValidators(validadores);
      docControl.updateValueAndValidity();
    });
  }

  onSubmit(): void {
    this.registerForm.markAllAsTouched();
    
    if (this.registerForm.invalid) {
      mostrarToast("Por favor, complete todos los campos obligatorios.", "warning");
      return;
    }
    
    this.isLoading = true;
    this.errorMessage = null;
    
    const formValues = this.registerForm.value;
    
    // --- MENTOR: LÓGICA INTELIGENTE TWILIO (+54 9) ---
    let telefonoFinal = '';
    let numeroLimpio = formValues.telefono.trim();
    let codigoPais = formValues.codigoPais;

    if (codigoPais === '+54' && !numeroLimpio.startsWith('9')) {
        telefonoFinal = `${codigoPais}9${numeroLimpio}`;
    } else {
        telefonoFinal = `${codigoPais}${numeroLimpio}`;
    }
    // -------------------------------------------------

    const request: RegisterRequestDTO = {
        nombre: formValues.nombre,
        apellido: formValues.apellido,
        email: formValues.email,
        password: formValues.password,
        tipoDocumentoId: formValues.tipoDocumentoId,
        documento: formValues.documento,
        telefono: telefonoFinal // <--- Número formateado
    };
    
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

  get f() { return this.registerForm.controls; }

  get selectedTipoDocNombre(): string {
    const tipoId = this.registerForm.get('tipoDocumentoId')?.value;
    if (!tipoId || !this.tiposDocumento.length) return '';
    const tipo = this.tiposDocumento.find(t => t.id === +tipoId);
    return tipo ? tipo.nombreCorto.toUpperCase() : '';
  }
}