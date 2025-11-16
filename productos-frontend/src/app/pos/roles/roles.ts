import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormArray } from '@angular/forms'; 
import { CommonModule } from '@angular/common'; 
import { RouterModule } from '@angular/router';
import { RolService } from '../../service/rol.service'; 
import { PermisoService } from '../../service/permiso.service'; 
import { RolDTO } from '../../models/rol.model'; 
import { PermisoDTO } from '../../models/permiso.model'; 
import { mostrarToast } from '../../utils/toast'; 
import { HttpErrorResponse } from '@angular/common/http';

import { forkJoin, Observable } from 'rxjs'; 
import { HasPermissionDirective } from '../../directives/has-permission.directive';

declare var bootstrap: any;

@Component({
  selector: 'app-roles',
  standalone: true, 
  imports: [
    CommonModule, 
    ReactiveFormsModule, 
    RouterModule,
    HasPermissionDirective
  ],
  templateUrl: './roles.html',
  styleUrls: ['./roles.css']
})
export default class RolesComponent implements OnInit {

  roles: RolDTO[] = [];
  permisosDisponibles: PermisoDTO[] = []; 
  rolForm: FormGroup;
  editMode: boolean = false;
  rolEditId: number | null = null;
  isLoading = false;
  errorMessage: string | null = null;

  private fb = inject(FormBuilder);
  private rolService = inject(RolService);
  private permisoService = inject(PermisoService); 

  constructor() {
    this.rolForm = this.fb.group({
      id: [null], 
      nombreRol: ['', [Validators.required, Validators.maxLength(50)]], 
      descripcion: ['', [Validators.maxLength(255)]],
      permisos: this.fb.array([])
    });
  } 

  ngOnInit() {
    this.cargarRolesYPermisos();
  }

  get permisosArray(): FormArray {
    return this.rolForm.get('permisos') as FormArray;
  }

  cargarRolesYPermisos() {
    this.isLoading = true;
    this.errorMessage = null;

    forkJoin({
        roles: this.rolService.listarRoles() as Observable<RolDTO[]>,
        permisos: this.permisoService.listarPermisos() as Observable<PermisoDTO[]>
    }).subscribe({
        next: (results: { roles: RolDTO[], permisos: PermisoDTO[] }) => {
            this.roles = results.roles;
            this.permisosDisponibles = results.permisos;
            this.isLoading = false;
        },
        error: (err: HttpErrorResponse) => {
            console.error("Error al cargar datos", err);
            this.errorMessage = err.error?.message || "Error al cargar roles y permisos.";
            if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
            this.isLoading = false;
        }
    });
  }

  private setupPermisosCheckboxes(permisosAsociados: PermisoDTO[] = []): void {
      this.permisosArray.clear();
      
      const permisosAsociadosNombres = new Set(permisosAsociados.map(p => p.nombrePermiso));

      this.permisosDisponibles.forEach(permiso => {
          const isChecked = permisosAsociadosNombres.has(permiso.nombrePermiso);
          this.permisosArray.push(this.fb.control(isChecked));
      });
  }

  abrirModalNuevo() {
    this.editMode = false;
    this.rolEditId = null;
    this.rolForm.reset({
      nombreRol: '',
      descripcion: ''
    });
    this.setupPermisosCheckboxes(); 
    const modal = new bootstrap.Modal(document.getElementById('rolModal'));
    modal.show();
  }

  editarRol(rol: RolDTO) {
    this.editMode = true;
    this.rolEditId = rol.id ?? null;
    
    this.rolForm.patchValue({
      id: rol.id,
      nombreRol: rol.nombreRol,
      descripcion: rol.descripcion
    });

    this.setupPermisosCheckboxes(rol.permisos);

    const modal = new bootstrap.Modal(document.getElementById('rolModal'));
    modal.show();
  }

  guardarRol() {
    this.rolForm.markAllAsTouched();
    if (this.rolForm.invalid) {
      mostrarToast("El nombre del rol es obligatorio.", "warning");
      return;
    }

    this.isLoading = true;
    this.errorMessage = null;
    
    const permisosSeleccionados: PermisoDTO[] = this.permisosArray.controls
        .map((control, i) => control.value ? this.permisosDisponibles[i] : null)
        .filter((p): p is PermisoDTO => p !== null);

    const rol: RolDTO = {
        ...this.rolForm.value,
        id: this.rolEditId,
        permisos: permisosSeleccionados 
    };
    
    // --- Mentor: INICIO DE LA CORRECCIÓN DE UX ---
    // 1. Determinar el mensaje de éxito antes de la llamada asíncrona
    const successMessage = this.editMode ? 'Rol actualizado con éxito.' : 'Rol creado con éxito.';
    
    const obs = this.editMode
      ? this.rolService.actualizar(rol.id!, rol)
      : this.rolService.crear(rol);

    obs.subscribe({
      next: () => {
        this.resetForm();
        this.cargarRolesYPermisos();
        this.cerrarModal();
        // 2. Usar el mensaje predefinido
        mostrarToast(successMessage, 'success'); 
        this.isLoading = false;
      },
      error: (err: HttpErrorResponse) => {
        console.error('Error al guardar', err);
        this.errorMessage = err.error?.message || 'Error al guardar el rol.';
        if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
        this.isLoading = false;
      }
    });
    // --- Mentor: FIN DE LA CORRECCIÓN DE UX ---
  }

  eliminarRol(id: number | undefined) {
    if (!id) return; 
    if (confirm('¿Seguro que deseas eliminar este ROL? (Acción permanente)')) {
      this.rolService.eliminar(id).subscribe({
        next: () => {
          this.cargarRolesYPermisos(); 
          mostrarToast('Rol eliminado', 'warning');
        },
        error: (err: HttpErrorResponse) => {
           console.error('Error al eliminar', err);
           mostrarToast(err.error?.message || 'Error al eliminar (¿está en uso por un usuario?)', 'danger');
        }
      });
    }
  }

  resetForm() {
    this.rolForm.reset({
      nombreRol: '',
      descripcion: ''
    });
    this.editMode = false;
    this.rolEditId = null;
  }

  cerrarModal() {
    const modalElement = document.getElementById('rolModal');
    if (modalElement) {
      const modal = bootstrap.Modal.getInstance(modalElement);
      if (modal) {
        modal.hide();
      }
    }
  }

  // Helper para validación
  get f() { return this.rolForm.controls; }
}