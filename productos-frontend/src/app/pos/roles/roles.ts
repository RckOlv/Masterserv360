import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common'; // Importar CommonModule
import { RouterModule } from '@angular/router';
import { RolService } from '../../service/rol.service'; // Importamos el RolService
import { RolDTO } from '../../models/rol.model';       // Importamos el RolDTO
import { mostrarToast } from '../../utils/toast';    // Asumo que tienes esto

// Declarar Bootstrap globalmente para el Modal
declare var bootstrap: any;

@Component({
  selector: 'app-roles',
  standalone: true, // ¡Asegúrate de que 'standalone' sea 'true'!
  imports: [
    CommonModule, 
    ReactiveFormsModule, // Necesario para [formGroup]
    RouterModule
  ],
  templateUrl: './roles.html',
  styleUrls: ['./roles.css'] // Corregido de styleUrl a styleUrls
})
export default class RolesComponent implements OnInit { // Añadimos 'default' y 'implements OnInit'

  roles: RolDTO[] = [];
  rolForm: FormGroup;  // Usamos FormGroup
  editMode: boolean = false;
  rolEditId: number | null = null;
  isLoading = false;
  errorMessage: string | null = null;

  // Inyección de dependencias moderna
  private fb = inject(FormBuilder);
  private rolService = inject(RolService);

  constructor() {
    // Inicializamos el formulario en el constructor
    this.rolForm = this.fb.group({
      // El formulario debe usar 'nombreRol' para coincidir con el DTO
      nombreRol: ['', [Validators.required, Validators.maxLength(50)]], 
      descripcion: ['', [Validators.maxLength(255)]]
    });
  } 

  ngOnInit() {
    this.cargarRoles();
  }

  cargarRoles() {
    this.isLoading = true;
    this.errorMessage = null;
    this.rolService.listarRoles().subscribe({
      next: (data: RolDTO[]) => {
        this.roles = data;
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error("Error al cargar roles", err);
        this.errorMessage = "Error al cargar los roles.";
        if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
        this.isLoading = false;
      }
    });
  }

  guardarRol() {
    if (this.rolForm.invalid) {
      this.rolForm.markAllAsTouched();
      mostrarToast("El nombre del rol es obligatorio.", "warning");
      return;
    }

    this.isLoading = true;
    const rol: RolDTO = this.rolForm.value; 

    if (this.editMode && this.rolEditId != null) {
      rol.id = this.rolEditId;
      this.rolService.actualizar(rol.id, rol).subscribe({ // El servicio 'actualizar' espera ID y DTO
        next: () => {
          this.resetForm();
          this.cargarRoles();
          this.cerrarModal();
          mostrarToast('Rol actualizado', 'success');
          this.isLoading = false;
        },
        error: (err: any) => {
          console.error('Error al actualizar', err);
          this.errorMessage = err.error?.message || 'Error al actualizar el rol.';
          if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
          this.isLoading = false;
        }
      });
    } else {
      this.rolService.crear(rol).subscribe({
        next: () => {
          this.resetForm();
          this.cargarRoles();
          this.cerrarModal();
          mostrarToast('Rol creado', 'success');
          this.isLoading = false;
        },
        error: (err: any) => {
           console.error('Error al crear', err);
           this.errorMessage = err.error?.message || 'Error al crear el rol.';
           if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
           this.isLoading = false;
        }
      });
    }
  }

  abrirModalNuevo() {
    this.editMode = false;
    this.rolEditId = null;
    this.rolForm.reset({
      nombreRol: '',
      descripcion: ''
    });
    const modal = new bootstrap.Modal(document.getElementById('rolModal'));
    modal.show();
  }

  editarRol(rol: RolDTO) {
    this.editMode = true;
    this.rolEditId = rol.id ?? null; 
    this.rolForm.patchValue({
      nombreRol: rol.nombreRol,
      descripcion: rol.descripcion
    });
    const modal = new bootstrap.Modal(document.getElementById('rolModal'));
    modal.show();
  }

  eliminarRol(id: number | undefined) {
    if (!id) return; 
    if (confirm('¿Seguro que deseas eliminar este ROL? (Acción permanente)')) {
      this.rolService.eliminar(id).subscribe({
        next: () => {
          this.cargarRoles(); 
          mostrarToast('Rol eliminado', 'warning');
        },
        error: (err: any) => {
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