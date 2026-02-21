import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CategoriaService } from '../../service/categoria.service';
import { CategoriaDTO } from '../../models/categoria.model';
import { mostrarToast, confirmarAccion } from '../../utils/toast'; // ✅ AÑADIDO confirmarAccion
import { HttpErrorResponse } from '@angular/common/http';
import { HasPermissionDirective } from '../../directives/has-permission.directive'; 

declare var bootstrap: any;

@Component({
  selector: 'app-categorias',
  standalone: true,
  templateUrl: './categorias.html',
  styleUrls: ['./categorias.css'], 
  imports: [
    CommonModule, 
    ReactiveFormsModule, 
    FormsModule,
    HasPermissionDirective 
  ], 
})
export default class CategoriasComponent implements OnInit {

  private fb = inject(FormBuilder);
  private categoriaService = inject(CategoriaService);

  categorias: CategoriaDTO[] = [];
  
  filtroForm: FormGroup;
  categoriaForm: FormGroup; 
  
  esEdicion = false;
  categoriaSeleccionadaId: number | null = null;
  isLoading = false;
  errorMessage: string | null = null;

  constructor() {
    this.filtroForm = this.fb.group({
      nombre: [''],
      estado: ['ACTIVO']
    });

    this.categoriaForm = this.fb.group({
      id: [null],
      nombre: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
      descripcion: ['', [Validators.maxLength(255)]],
      estado: ['ACTIVO'] 
    });
  }

  ngOnInit() {
    this.listarCategorias();
  }

  listarCategorias() {
    this.isLoading = true;
    this.errorMessage = null;
    
    const { nombre, estado } = this.filtroForm.value;

    this.categoriaService.listarCategorias(estado).subscribe({
      next: (data: CategoriaDTO[]) => {
        const termino = nombre ? nombre.toLowerCase().trim() : '';
        if (!termino) {
          this.categorias = [...data];
        } else {
          this.categorias = data.filter((cat) => 
            cat.nombre.toLowerCase().includes(termino)
          );
        }
        this.isLoading = false;
      },
      error: (err: HttpErrorResponse) => {
        console.error('Error al listar categorías:', err);
        this.handleError(err, 'cargar');
        this.isLoading = false;
      },
    });
  }

  aplicarFiltros(): void {
    this.listarCategorias();
  }

  reiniciarFiltros() {
    this.filtroForm.reset({
      nombre: '',
      estado: 'ACTIVO'
    });
    this.listarCategorias();
    mostrarToast('Filtros reiniciados', 'info');
  }

  abrirModalNuevo() {
    this.esEdicion = false;
    this.categoriaSeleccionadaId = null;
    this.categoriaForm.reset({ id: null, nombre: '', descripcion: '', estado: 'ACTIVO' });
    const modal = new bootstrap.Modal(document.getElementById('categoriaModal'));
    modal.show();
  }

  abrirModalEditar(categoria: CategoriaDTO) {
    if (!categoria.id) return;
    this.esEdicion = true;
    this.categoriaSeleccionadaId = categoria.id;
    this.categoriaForm.patchValue({
      id: categoria.id,
      nombre: categoria.nombre,
      descripcion: categoria.descripcion,
      estado: categoria.estado || 'ACTIVO'
    });
    const modal = new bootstrap.Modal(document.getElementById('categoriaModal'));
    modal.show();
  }

  guardarCategoria() {
    this.categoriaForm.markAllAsTouched();
    
    if (this.categoriaForm.invalid) {
        mostrarToast('Revise los campos obligatorios.', 'warning');
        return;
    }

    this.isLoading = true; 
    this.errorMessage = null;
    const categoriaData = this.categoriaForm.value as CategoriaDTO;
    
    if (this.esEdicion && this.categoriaSeleccionadaId) {
      categoriaData.id = this.categoriaSeleccionadaId;
    }

    const obs = this.esEdicion
      ? this.categoriaService.actualizar(categoriaData)
      : this.categoriaService.crear(categoriaData);

    obs.subscribe({
      next: (categoriaGuardada: CategoriaDTO) => {
        this.listarCategorias(); 
        mostrarToast(this.esEdicion ? 'Categoría actualizada' : 'Categoría creada', 'success');
        this.cerrarModal();
        this.isLoading = false; 
      },
      error: (err: HttpErrorResponse) => {
        console.error('Error al guardar categoría:', err);
        this.handleError(err, 'guardar');
        this.isLoading = false;
      },
    });
  }

  cerrarModal() {
    const modalElement = document.getElementById('categoriaModal');
    if (modalElement) {
      const modal = bootstrap.Modal.getInstance(modalElement);
      if (modal) modal.hide();
    }
  }

  /** ✅ ELIMINAR (DESACTIVAR) CATEGORÍA MIGRADO */
  eliminarCategoria(id?: number) {
    if (!id) return;
    
    confirmarAccion(
      'Desactivar Categoría', 
      '¿Estás seguro de marcar esta categoría como INACTIVA?'
    ).then((confirmado) => {
      if (confirmado) {
        this.isLoading = true;
        this.errorMessage = null;
        this.categoriaService.softDelete(id).subscribe({
          next: () => {
            this.listarCategorias(); 
            mostrarToast('Categoría marcada como inactiva', 'warning');
          },
          error: (err: HttpErrorResponse) => {
            console.error('Error al eliminar categoría:', err);
            this.handleError(err, 'eliminar');
            this.isLoading = false;
          },
        });
      }
    });
  }

  /** ✅ REACTIVAR CATEGORÍA MIGRADO */
  reactivarCategoria(id?: number) {
     if (!id) return;
     
     confirmarAccion(
       'Reactivar Categoría', 
       '¿Estás seguro de REACTIVAR esta categoría?'
     ).then((confirmado) => {
       if (confirmado) {
         this.isLoading = true;
         this.errorMessage = null;
         this.categoriaService.reactivar(id).subscribe({
           next: () => {
             this.listarCategorias(); 
             mostrarToast('Categoría reactivada correctamente', 'success');
           },
           error: (err: HttpErrorResponse) => {
             console.error('Error al reactivar categoría:', err);
             this.handleError(err, 'reactivar');
             this.isLoading = false;
           },
         });
       }
     });
   }

  // --- MENTOR: REFACTORIZACIÓN DE ERRORES ---
  private handleError(err: HttpErrorResponse, context: string) {
    if (err.status === 403) {
      this.errorMessage = 'Acción no permitida: No tiene permisos de Administrador.';
    } 
    // AQUÍ EL CAMBIO CLAVE: Ya no asumimos que 500 = "Error interno" ciegamente
    else {
        // Intentamos sacar el mensaje real primero
        if (err.error && typeof err.error === 'string') {
            this.errorMessage = err.error;
        } else if (err.error && err.error.message) {
            this.errorMessage = err.error.message;
        } else {
            // Si no hay mensaje, ahí sí ponemos el genérico
            this.errorMessage = `Ocurrió un error al ${context} la categoría.`;
        }
    }
    
    if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
  }
  // -------------------------------------------

  get f() { return this.categoriaForm.controls; }
}