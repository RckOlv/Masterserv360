import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormArray, AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms'; 
import { CommonModule } from '@angular/common'; 
import { RouterModule } from '@angular/router';
import { RolService } from '../../service/rol.service'; 
import { PermisoService } from '../../service/permiso.service'; 
import { RolDTO } from '../../models/rol.model'; 
import { PermisoDTO } from '../../models/permiso.model'; 
import { mostrarToast, confirmarAccion } from '../../utils/toast'; // ✅ AÑADIDO confirmarAccion
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
    // MENTOR: Validación de nombre (empieza con ROLE_) y validación de permisos
    this.rolForm = this.fb.group({
      id: [null], 
      nombreRol: ['', [Validators.required, Validators.minLength(5), Validators.pattern(/^ROLE_[A-Z_]+$/)]], 
      descripcion: ['', [Validators.maxLength(255)]],
      permisos: this.fb.array([], this.atLeastOnePermissionValidator) // Validador personalizado
    });
  } 

  ngOnInit() {
    this.cargarRolesYPermisos();
  }

  get permisosArray(): FormArray {
    return this.rolForm.get('permisos') as FormArray;
  }

  // MENTOR: Validador para asegurar que al menos un permiso esté seleccionado
  atLeastOnePermissionValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
      const formArray = control as FormArray;
      const hasPermission = formArray.controls.some(ctrl => ctrl.value === true);
      return hasPermission ? null : { noPermissionSelected: true };
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
      nombreRol: 'ROLE_', // Pre-llenamos el prefijo para ayudar al usuario
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
      // Si el error es de permisos vacíos, mostramos mensaje específico
      if (this.permisosArray.errors?.['noPermissionSelected']) {
          mostrarToast("Debe asignar al menos un permiso al rol.", "warning");
      } else {
          mostrarToast("Revise los campos obligatorios.", "warning");
      }
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
    
    const successMessage = this.editMode ? 'Rol actualizado con éxito.' : 'Rol creado con éxito.';
    
    const obs = this.editMode
      ? this.rolService.actualizar(rol.id!, rol)
      : this.rolService.crear(rol);

    obs.subscribe({
      next: () => {
        this.resetForm();
        this.cargarRolesYPermisos();
        this.cerrarModal();
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
  }

  /** ✅ ELIMINAR ROL MIGRADO */
  eliminarRol(id: number | undefined) {
    if (!id) return; 
    
    confirmarAccion(
      'Eliminar Rol', 
      '¿Seguro que deseas eliminar este ROL? (Acción permanente)'
    ).then((confirmado) => {
      if (confirmado) {
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
    });
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

  get f() { return this.rolForm.controls; }
}