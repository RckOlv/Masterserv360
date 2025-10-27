import { Component, OnInit, inject } from '@angular/core'; // Usar inject
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { RouterModule } from '@angular/router';
import { PermisoService } from '../../service/permiso.service';
import { PermisoDTO } from '../../models/permiso.model';
// import { mostrarToast } from '../../utils/toast'; // Asumo que tienes esto

@Component({
  selector: 'app-permiso',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, HttpClientModule, RouterModule],
  templateUrl: './permisos.html',
  styleUrls: ['./permisos.css']
})
export default class PermisoComponent implements OnInit { // <-- Añadido 'default'
  permisos: PermisoDTO[] = [];
  permisoForm!: FormGroup;
  editMode: boolean = false;
  permisoEditId: number | null = null; // <-- Usar null

  // Inyección moderna
  private fb = inject(FormBuilder);
  private permisoService = inject(PermisoService);

  constructor() {} // Constructor limpio

  ngOnInit() {
    this.cargarPermisos();
    // CORREGIDO: El formulario debe usar 'nombrePermiso' para coincidir con el DTO
    this.permisoForm = this.fb.group({
      nombrePermiso: ['', Validators.required], 
      descripcion: ['']
    });
  }

  cargarPermisos() {
    this.permisoService.listarPermisos().subscribe({
      next: (data: PermisoDTO[]) => this.permisos = data, // Tipado
      error: (err: any) => console.error("Error al cargar permisos", err) // Tipado
    });
  }

  guardarPermiso() {
    if (this.permisoForm.invalid) {
      this.permisoForm.markAllAsTouched();
      return;
    }

    // El .value del form ahora coincide con PermisoDTO
    const permiso: PermisoDTO = this.permisoForm.value; 

    if (this.editMode && this.permisoEditId != null) {
      permiso.id = this.permisoEditId;
      // CORREGIDO: Llamar al método 'actualizar'
      this.permisoService.actualizar(permiso).subscribe({
        next: () => {
          this.resetForm();
          this.cargarPermisos();
          // mostrarToast('Permiso actualizado', 'success');
        },
        error: (err: any) => console.error('Error al actualizar', err)
      });
    } else {
      // CORREGIDO: Llamar al método 'crear'
      this.permisoService.crear(permiso).subscribe({
        next: () => {
          this.resetForm();
          this.cargarPermisos();
          // mostrarToast('Permiso creado', 'success');
        },
        error: (err: any) => console.error('Error al crear', err)
      });
    }
  }

  editarPermiso(permiso: PermisoDTO) {
    this.editMode = true;
    this.permisoEditId = permiso.id ?? null; // Asignación segura
    // CORREGIDO: El patchValue ahora coincide con el DTO y el Form
    this.permisoForm.patchValue({
      nombrePermiso: permiso.nombrePermiso,
      descripcion: permiso.descripcion
    });
  }

  eliminarPermiso(id: number | undefined) { // <-- Aceptar undefined
    if (!id) return; // Chequeo de seguridad
    if (confirm('¿Seguro que deseas eliminar este permiso?')) {
      // CORREGIDO: Llamar al método 'softDelete'
      this.permisoService.softDelete(id).subscribe({
        next: () => {
          this.cargarPermisos();
          // mostrarToast('Permiso eliminado', 'warning');
        },
        error: (err: any) => console.error('Error al eliminar', err)
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
}