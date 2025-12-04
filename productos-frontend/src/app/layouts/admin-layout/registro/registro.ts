import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router'; 
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';

import { UsuarioService } from '../../../service/usuario.service';
import { RolService } from '../../../service/rol.service';
import { TipoDocumentoService } from '../../../service/tipo-documento.service';
import { RolDTO } from '../../../models/rol.model';
import { TipoDocumentoDTO } from '../../../models/tipo-documento.model';
import { UsuarioDTO } from '../../../models/usuario.model';
import { mostrarToast } from '../../../utils/toast';
import { HasPermissionDirective } from '../../../directives/has-permission.directive';

@Component({
  standalone: true,
  selector: 'app-registro-admin', 
  templateUrl: './registro.html',
  styleUrls: ['./registro.css'],
  imports: [
    CommonModule, 
    ReactiveFormsModule, 
    RouterLink,
    HasPermissionDirective
  ]
})
export default class RegistroAdminComponent implements OnInit {

  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute); 
  private usuarioService = inject(UsuarioService);
  private rolService = inject(RolService);
  private tipoDocumentoService = inject(TipoDocumentoService);

  public userForm: FormGroup;
  public errorMessage: string | null = null;
  public roles: RolDTO[] = [];
  public tiposDocumento: TipoDocumentoDTO[] = [];
  
  public esEdicion = false;
  public usuarioId: number | null = null;
  public pageTitle = 'Registrar Nuevo Usuario';
  public isSubmitting = false;

  // Validaciones y UI
  public maxDocumentoLength: number = 20; 
  public hidePassword = true;
  public hideConfirmPassword = true;

  // Lista de Países
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

    this.userForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.minLength(2), Validators.pattern(textPattern)]],
      apellido: ['', [Validators.required, Validators.minLength(2), Validators.pattern(textPattern)]],
      email: ['', [Validators.required, Validators.pattern(emailPattern)]],
      
      password: [''], 
      confirmPassword: [''], 

      tipoDocumentoId: [null, Validators.required],
      documento: ['', Validators.required], 
      
      // Teléfono dividido
      codigoPais: ['+54', Validators.required],
      telefono: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(15)]],
      
      rolId: [null, Validators.required], 
      estado: ['ACTIVO', Validators.required]
    }, { validators: this.passwordMatchValidator });
  }

  ngOnInit(): void {
    this.cargarDropdowns();

    this.userForm.get('tipoDocumentoId')?.valueChanges.subscribe(tipoId => {
      if (this.tiposDocumento.length > 0) {
        this.actualizarValidadoresDocumento(tipoId);
      }
    });

    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.esEdicion = true;
        this.usuarioId = +id;
        this.pageTitle = 'Editar Usuario';
        
        this.userForm.get('password')?.clearValidators();
        this.userForm.get('password')?.updateValueAndValidity();
        this.userForm.get('confirmPassword')?.clearValidators();
        this.userForm.get('confirmPassword')?.updateValueAndValidity();

        this.cargarDatosUsuario(this.usuarioId);
      } else {
        this.userForm.get('password')?.setValidators([Validators.required, Validators.minLength(8)]);
        this.userForm.get('confirmPassword')?.setValidators([Validators.required]);
        
        this.userForm.get('password')?.updateValueAndValidity();
        this.userForm.get('confirmPassword')?.updateValueAndValidity();
      }
    });
  }

  togglePasswordVisibility() { this.hidePassword = !this.hidePassword; }
  toggleConfirmPasswordVisibility() { this.hideConfirmPassword = !this.hideConfirmPassword; }

  passwordMatchValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
    const pass = control.get('password')?.value;
    const confirm = control.get('confirmPassword')?.value;
    if (this.esEdicion && !pass && !confirm) return null;
    return pass === confirm ? null : { mismatch: true };
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
    if (controlName && this.userForm.get(controlName)) {
        this.userForm.get(controlName)?.setValue(input.value);
    }
  }

  cargarDropdowns(): void {
    this.tipoDocumentoService.listarTiposDocumento().subscribe({
        next: data => {
          this.tiposDocumento = data;
          if (this.esEdicion && this.userForm.get('tipoDocumentoId')?.value) {
            this.actualizarValidadoresDocumento(this.userForm.get('tipoDocumentoId')?.value);
          }
        },
        error: err => console.error('Error cargando tipos de documento', err)
    });

    this.rolService.listarRoles().subscribe({
        next: data => this.roles = data,
        error: err => console.error('Error cargando roles', err)
    });
  }

  actualizarValidadoresDocumento(tipoId: number | string | null): void {
    const documentoControl = this.userForm.get('documento');
    if (!documentoControl) return;

    const tipoDoc = this.tiposDocumento.find(t => t.id === +tipoId!);
    let validadores = [Validators.required];

    if (tipoDoc) {
      if (tipoDoc.nombreCorto === 'DNI') {
        this.maxDocumentoLength = 8;
        validadores.push(Validators.minLength(7), Validators.maxLength(8));
      } else if (tipoDoc.nombreCorto === 'CUIT') {
        this.maxDocumentoLength = 11;
        validadores.push(Validators.minLength(11), Validators.maxLength(11));
      } else {
        this.maxDocumentoLength = 20;
      }
    }
    
    documentoControl.setValidators(validadores);
    documentoControl.updateValueAndValidity(); 
  }

  cargarDatosUsuario(id: number): void {
    this.usuarioService.getById(id).subscribe({
      next: (usuario) => {
        
        let telefonoFull = usuario.telefono || '';
        let codigo = '+54';
        let numero = telefonoFull;

        if (telefonoFull.startsWith('+549')) {
            codigo = '+54';
            numero = telefonoFull.substring(4); 
        } else {
            const paisEncontrado = this.paises.find(p => telefonoFull.startsWith(p.codigo));
            if (paisEncontrado) {
                codigo = paisEncontrado.codigo;
                numero = telefonoFull.substring(codigo.length);
            }
        }

        this.userForm.patchValue({
          nombre: usuario.nombre,
          apellido: usuario.apellido,
          email: usuario.email,
          password: '',
          confirmPassword: '',
          tipoDocumentoId: usuario.tipoDocumentoId,
          documento: usuario.documento,
          rolId: usuario.roles && usuario.roles.length > 0 ? usuario.roles[0].id : null, 
          estado: usuario.estado,
          codigoPais: codigo,
          telefono: numero
        });
        
        if (this.tiposDocumento.length > 0 && usuario.tipoDocumentoId) {
          this.actualizarValidadoresDocumento(usuario.tipoDocumentoId);
        }
      },
      error: (err: any) => {
          mostrarToast('Error al cargar datos del usuario', 'danger');
          this.errorMessage = err.error?.message;
      }
    });
  }

  onSubmit(): void {
    this.userForm.markAllAsTouched();
    if (this.userForm.invalid) {
      mostrarToast("Por favor, complete todos los campos correctamente.", "warning");
      return;
    }

    this.isSubmitting = true;
    const formValue = this.userForm.value;

    let telefonoFinal = '';
    let numeroLimpio = formValue.telefono.trim();
    let codigoPais = formValue.codigoPais;

    if (codigoPais === '+54' && !numeroLimpio.startsWith('9')) {
        telefonoFinal = `${codigoPais}9${numeroLimpio}`;
    } else {
        telefonoFinal = `${codigoPais}${numeroLimpio}`;
    }

    const usuarioDTO: UsuarioDTO = {
        id: this.usuarioId ?? undefined, 
        nombre: formValue.nombre,
        apellido: formValue.apellido,
        email: formValue.email,
        passwordHash: formValue.password, 
        documento: formValue.documento,
        telefono: telefonoFinal,
        tipoDocumentoId: formValue.tipoDocumentoId,
        roles: [{ id: formValue.rolId, nombreRol: '', descripcion: '' }],
        estado: formValue.estado
    };

    const obs = this.esEdicion
      ? this.usuarioService.actualizarUsuarioAdmin(this.usuarioId!, usuarioDTO)
      : this.usuarioService.crearUsuarioAdmin(usuarioDTO);

    obs.subscribe({
      next: () => {
        this.isSubmitting = false;
        mostrarToast(this.esEdicion ? 'Usuario actualizado' : 'Usuario creado', 'success'); 
        this.router.navigate(['/pos/usuarios']);
      },
      error: (err: HttpErrorResponse) => {
        this.isSubmitting = false;
        console.error('Error al guardar usuario:', err);
        this.errorMessage = err.error?.message || 'Error al guardar el usuario.';
        if(this.errorMessage) mostrarToast(this.errorMessage, 'danger');
      }
    });
  }

  get f() { return this.userForm.controls; }

  get selectedTipoDocNombre(): string {
    const tipoId = this.userForm.get('tipoDocumentoId')?.value;
    if (!tipoId || !this.tiposDocumento.length) return '';
    const tipo = this.tiposDocumento.find(t => t.id === +tipoId);
    return tipo ? tipo.nombreCorto.toUpperCase() : '';
  }
}