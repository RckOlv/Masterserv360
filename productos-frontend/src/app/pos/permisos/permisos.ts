import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms'; // Mentor: Importar FormsModule
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { RouterModule } from '@angular/router';
import { PermisoService } from '../../service/permiso.service';
import { PermisoDTO } from '../../models/permiso.model';
import { mostrarToast } from '../../utils/toast'; 
import { HttpErrorResponse } from '@angular/common/http';
import { HasPermissionDirective } from '../../directives/has-permission.directive'; 

// Mentor: Declarar bootstrap para el modal
declare var bootstrap: any;

@Component({
  selector: 'app-permiso',
  standalone: true,
  imports: [
    CommonModule, 
    ReactiveFormsModule, 
    HttpClientModule, 
    RouterModule,
    FormsModule, // Mentor: Añadido para [(ngModel)]
    HasPermissionDirective
  ],
  templateUrl: './permisos.html',
  styleUrls: ['./permisos.css']
})
export default class PermisoComponent implements OnInit { 
  
  permisos: PermisoDTO[] = []; // Lista completa
  permisosFiltrados: PermisoDTO[] = []; // Lista para la tabla
  filtroNombre: string = ''; // Para el buscador
  
  permisoForm!: FormGroup;
  editMode: boolean = false;
  permisoEditId: number | null = null; 

  isLoading = false;
  isSubmitting = false;
  errorMessage: string | null = null;

  private fb = inject(FormBuilder);
  private permisoService = inject(PermisoService);

  constructor() {} 

  ngOnInit() {
    this.cargarPermisos();
    this.permisoForm = this.fb.group({
      nombrePermiso: ['', [Validators.required, Validators.maxLength(50)]], 
      descripcion: ['', [Validators.maxLength(255)]]
    });
  }

  cargarPermisos() {
    this.isLoading = true;
    this.errorMessage = null;
    this.permisoService.listarPermisos().subscribe({
      next: (data: PermisoDTO[]) => {
        this.permisos = data;
        this.filtrarLocalmente(); // Aplicar filtro
        this.isLoading = false;
      },
      error: (err: HttpErrorResponse) => { 
        console.error("Error al cargar permisos", err);
        this.errorMessage = err.error?.message || "Error al cargar permisos.";
        if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
        this.isLoading = false;
      }
    });
  }

  /** Filtra localmente por nombre de permiso */
  filtrarLocalmente() {
    const termino = this.filtroNombre.toLowerCase().trim();
    if (!termino) {
      this.permisosFiltrados = [...this.permisos];
    } else {
      this.permisosFiltrados = this.permisos.filter((p) => 
        p.nombrePermiso.toLowerCase().includes(termino) ||
        (p.descripcion && p.descripcion.toLowerCase().includes(termino))
      );
    }
  }

  // --- Mentor: Métodos de Modal ---

  abrirModalNuevo() {
    this.editMode = false;
    this.permisoEditId = null;
    this.errorMessage = null;
    this.permisoForm.reset({
      nombrePermiso: '',
      descripcion: ''
    });
    const modal = new bootstrap.Modal(document.getElementById('permisoModal'));
    modal.show();
  }

  abrirModalEditar(permiso: PermisoDTO) {
    this.editMode = true;
    this.permisoEditId = permiso.id ?? null;
    this.errorMessage = null;
    this.permisoForm.patchValue({
      nombrePermiso: permiso.nombrePermiso,
      descripcion: permiso.descripcion
    });
    const modal = new bootstrap.Modal(document.getElementById('permisoModal'));
    modal.show();
  }
  
  cerrarModal() {
    const modalElement = document.getElementById('permisoModal');
    if (modalElement) {
      const modal = bootstrap.Modal.getInstance(modalElement);
      if (modal) modal.hide();
    }
  }

  guardarPermiso() {
    if (this.permisoForm.invalid) {
      this.permisoForm.markAllAsTouched();
      mostrarToast("El nombre del permiso es obligatorio.", "warning");
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = null; 
    const permiso: PermisoDTO = this.permisoForm.value; 

    if (this.editMode && this.permisoEditId != null) {
      permiso.id = this.permisoEditId;
      this.permisoService.actualizar(permiso).subscribe({
        next: () => {
          this.resetForm();
          this.cargarPermisos();
          this.cerrarModal(); // Mentor: Cierra el modal
          mostrarToast('Permiso actualizado', 'success');
          this.isSubmitting = false;
        },
        error: (err: HttpErrorResponse) => {
          console.error('Error al actualizar', err);
          this.errorMessage = err.error?.message || "Error al actualizar el permiso.";
          // No cerramos el modal, mostramos el error dentro
          this.isSubmitting = false;
        }
      });
    } else {
      this.permisoService.crear(permiso).subscribe({
        next: () => {
          this.resetForm();
          this.cargarPermisos();
          this.cerrarModal(); // Mentor: Cierra el modal
          mostrarToast('Permiso creado', 'success');
          this.isSubmitting = false;
        },
        error: (err: HttpErrorResponse) => {
          console.error('Error al crear', err);
          this.errorMessage = err.error?.message || "Error al crear el permiso.";
          // No cerramos el modal, mostramos el error dentro
          this.isSubmitting = false;
        }
      });
    }
  }

  eliminarPermiso(id: number | undefined) { 
    if (!id) return; 
    if (confirm('¿Seguro que deseas eliminar este permiso? Esta acción es PERMANENTE y puede romper el sistema si está en uso.')) {
      this.permisoService.softDelete(id).subscribe({ 
        next: () => {
          this.cargarPermisos();
          mostrarToast('Permiso eliminado (marcado como inactivo)', 'warning');
        },
        error: (err: HttpErrorResponse) => {
          console.error('Error al eliminar', err);
          mostrarToast(err.error?.message || 'Error al eliminar el permiso.', 'danger');
        }
      });
    }
  }

  resetForm() {
    this.permisoForm.reset({
      nombrePermiso: '',
      descripcion: ''
    });
    this.editMode = false;
    this.permisoEditId = null;
  }
  
  // Helper para validación
  get f() { return this.permisoForm.controls; }
}