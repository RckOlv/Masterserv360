import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router'; 
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http'; // Importar HttpErrorResponse

import { UsuarioService } from '../../../service/usuario.service';
import { RolService } from '../../../service/rol.service';
import { TipoDocumentoService } from '../../../service/tipo-documento.service';
import { RolDTO } from '../../../models/rol.model';
import { TipoDocumentoDTO } from '../../../models/tipo-documento.model';
import { UsuarioDTO } from '../../../models/usuario.model';
import { mostrarToast } from '../../../utils/toast';
import { HasPermissionDirective } from '../../../directives/has-permission.directive'; // Importar Directiva

@Component({
  standalone: true,
  selector: 'app-registro-admin', 
  templateUrl: './registro.html',
  styleUrls: ['./registro.css'],
  imports: [
    CommonModule, 
    ReactiveFormsModule, 
    RouterLink,
    HasPermissionDirective // Añadir Directiva
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
  public roles: RolDTO[] = []; // Esta lista será filtrada
  public tiposDocumento: TipoDocumentoDTO[] = [];
  
  public esEdicion = false;
  public usuarioId: number | null = null;
  public pageTitle = 'Registrar Nuevo Usuario';
  public isSubmitting = false;

  // Mapa de Validadores Dinámicos
  private documentoValidatorMap = {
    'DNI': [Validators.required, Validators.pattern('^[0-9]{7,8}$')],
    'CUIT': [Validators.required, Validators.pattern('^[0-9]{11}$')],
    'OTRO': [Validators.required] 
  };

  constructor() {
    this.userForm = this.fb.group({
      nombre: ['', Validators.required],
      apellido: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.minLength(8)]], 
      tipoDocumentoId: [null, Validators.required],
      documento: ['', Validators.required], 
      telefono: ['', Validators.required],
      rolId: [null, Validators.required], 
      estado: ['ACTIVO', Validators.required]
    });
  }

  ngOnInit(): void {
    this.cargarDropdowns();

    // Suscribirse a cambios en el Tipo de Documento
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
        this.cargarDatosUsuario(this.usuarioId);
      } else {
        this.userForm.get('password')?.setValidators([Validators.required, Validators.minLength(8)]);
        this.userForm.get('password')?.updateValueAndValidity();
      }
    });
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
        next: data => {
          this.roles = data; // <--- FILTRO ELIMINADO
        },
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
        validadores = this.documentoValidatorMap['DNI'];
      } else if (tipoDoc.nombreCorto === 'CUIT') {
        validadores = this.documentoValidatorMap['CUIT'];
      }
    }
    
    documentoControl.setValidators(validadores);
    documentoControl.updateValueAndValidity(); 
  }

  cargarDatosUsuario(id: number): void {
    this.usuarioService.getById(id).subscribe({
      next: (usuario) => {
        this.userForm.patchValue({
          nombre: usuario.nombre,
          apellido: usuario.apellido,
          email: usuario.email,
          password: '',
          tipoDocumentoId: usuario.tipoDocumentoId,
          documento: usuario.documento,
          telefono: usuario.telefono,
          rolId: usuario.roles && usuario.roles.length > 0 ? usuario.roles[0].id : null, 
          estado: usuario.estado
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
      mostrarToast("Por favor, complete todos los campos obligatorios.", "warning");
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = null;
    const formValue = this.userForm.value;

    const usuarioDTO: UsuarioDTO = {
        id: this.usuarioId ?? undefined, 
        nombre: formValue.nombre,
        apellido: formValue.apellido,
        email: formValue.email,
        passwordHash: formValue.password, 
        documento: formValue.documento,
        telefono: formValue.telefono,
        tipoDocumentoId: formValue.tipoDocumentoId,
        roles: [{
          id: formValue.rolId, 
          nombreRol: '',
          descripcion: ''
        }],
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
        this.errorMessage = err.error?.message || 'Error al guardar el usuario. Verifique si el email o documento ya existen.';
        if(this.errorMessage) mostrarToast(this.errorMessage, 'danger');
      }
    });
  }

  get f() { return this.userForm.controls; }

  // --- Mentor: INICIO DE LA MODIFICACIÓN ---
  // 7. Getter para los mensajes de error dinámicos (copiado de reg-cli.ts)
  get selectedTipoDocNombre(): string {
    const tipoId = this.userForm.get('tipoDocumentoId')?.value;
    if (!tipoId || !this.tiposDocumento.length) {
      return '';
    }
    const tipo = this.tiposDocumento.find(t => t.id === +tipoId);
    return tipo ? tipo.nombreCorto.toUpperCase() : '';
  }
  // --- Mentor: FIN DE LA MODIFICACIÓN ---
}