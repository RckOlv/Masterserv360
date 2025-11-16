import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router'; // Importar ActivatedRoute
import { CommonModule } from '@angular/common';

import { UsuarioService } from '../../../service/usuario.service';
import { RolService } from '../../../service/rol.service';
import { TipoDocumentoService } from '../../../service/tipo-documento.service';
import { RegisterRequestDTO } from '../../../models/register-request.model';
import { RolDTO } from '../../../models/rol.model';
import { TipoDocumentoDTO } from '../../../models/tipo-documento.model';
import { UsuarioDTO } from '../../../models/usuario.model';
import { mostrarToast } from '../../../utils/toast';

@Component({
  standalone: true,
  selector: 'app-registro-admin', 
  templateUrl: './registro.html',
  styleUrls: ['./registro.css'],
  imports: [
    CommonModule, 
    ReactiveFormsModule, 
    RouterLink 
  ]
})
export default class RegistroAdminComponent implements OnInit {

  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute); // Para modo edición
  private usuarioService = inject(UsuarioService);
  private rolService = inject(RolService);
  private tipoDocumentoService = inject(TipoDocumentoService);

  public userForm: FormGroup; // Cambiado a nombre genérico
  public errorMessage: string | null = null;
  public roles: RolDTO[] = [];
  public tiposDocumento: TipoDocumentoDTO[] = [];
  
  public esEdicion = false;
  public usuarioId: number | null = null;
  public pageTitle = 'Registrar Nuevo Usuario';

  constructor() {
    this.userForm = this.fb.group({
      nombre: ['', Validators.required],
      apellido: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.minLength(8)]], // Opcional en edición
      tipoDocumentoId: [null, Validators.required],
      documento: ['', Validators.required],
      telefono: ['', Validators.required],
      rolId: [null, Validators.required], // El ID del Rol (simple)
      estado: ['ACTIVO', Validators.required]
    });
  }

  ngOnInit(): void {
    this.cargarDropdowns();

    // Comprobar si estamos en modo EDICIÓN
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.esEdicion = true;
        this.usuarioId = +id;
        this.pageTitle = 'Editar Usuario';
        // Si es edición, la contraseña no es obligatoria
        this.userForm.get('password')?.clearValidators();
        this.userForm.get('password')?.updateValueAndValidity();
        this.cargarDatosUsuario(this.usuarioId);
      } else {
        // Si es CREACIÓN, la contraseña SÍ es obligatoria
        this.userForm.get('password')?.setValidators([Validators.required, Validators.minLength(8)]);
        this.userForm.get('password')?.updateValueAndValidity();
      }
    });
  }

  cargarDropdowns(): void {
    this.tipoDocumentoService.listarTiposDocumento().subscribe({
        next: data => this.tiposDocumento = data,
        error: err => console.error('Error cargando tipos de documento', err)
    });
    this.rolService.listarRoles().subscribe({
        next: data => this.roles = data,
        error: err => console.error('Error cargando roles', err)
    });
  }

  cargarDatosUsuario(id: number): void {
    this.usuarioService.getById(id).subscribe({
      next: (usuario) => {
        this.userForm.patchValue({
          nombre: usuario.nombre,
          apellido: usuario.apellido,
          email: usuario.email,
          password: '', // Dejar vacío
          tipoDocumentoId: usuario.tipoDocumentoId,
          documento: usuario.documento,
          telefono: usuario.telefono,
          // Mapeamos el *primer* rol del array (simplificación)
          rolId: usuario.roles && usuario.roles.length > 0 ? usuario.roles[0].id : null, 
          estado: usuario.estado
        });
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

    this.errorMessage = null;
    const formValue = this.userForm.value;

    // 1. Crear el DTO que el backend espera (UsuarioDTO)
    const usuarioDTO: UsuarioDTO = {
        id: this.usuarioId ?? undefined, // Añadir ID si estamos editando
        nombre: formValue.nombre,
        apellido: formValue.apellido,
        email: formValue.email,
        passwordHash: formValue.password, // Se envía vacío si no se edita
        documento: formValue.documento,
        telefono: formValue.telefono,
        tipoDocumentoId: formValue.tipoDocumentoId,
        // 2. Mapeamos el rolId seleccionado
        roles: [{
          id: formValue.rolId, nombreRol: '',
          descripcion: ''
        }],
        estado: formValue.estado
    };

    // 3. Decidir si crear o actualizar
    const obs = this.esEdicion
      ? this.usuarioService.actualizarUsuarioAdmin(this.usuarioId!, usuarioDTO)
      : this.usuarioService.crearUsuarioAdmin(usuarioDTO);

    obs.subscribe({
      next: () => {
        mostrarToast(this.esEdicion ? 'Usuario actualizado' : 'Usuario creado', 'success'); 
        this.router.navigate(['/usuarios']); // Vuelve a la lista
      },
      error: (err: any) => {
        console.error('Error al guardar usuario:', err);
        this.errorMessage = err.error?.message || 'Error al guardar el usuario.';
        if(this.errorMessage) mostrarToast(this.errorMessage, 'danger');
      }
    });
  }

  get f() { return this.userForm.controls; }
}