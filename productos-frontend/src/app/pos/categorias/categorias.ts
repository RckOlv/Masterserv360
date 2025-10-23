import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormsModule } from '@angular/forms';
import { CategoriaService } from '../../service/categoria.service';
import { CategoriaDTO } from '../../models/categoria.model';
import { mostrarToast } from '../../utils/toast';

// Declarar Bootstrap globalmente
declare var bootstrap: any;

@Component({
  selector: 'app-categorias',
  standalone: true,
  templateUrl: './categorias.html',
  styleUrls: ['./categorias.css'],
  imports: [CommonModule, ReactiveFormsModule,FormsModule],
})
export default class CategoriasComponent implements OnInit {

  private fb = inject(FormBuilder);
  private categoriaService = inject(CategoriaService);

  // Estado
  categorias: CategoriaDTO[] = []; // Lista completa del backend
  categoriasFiltradas: CategoriaDTO[] = []; // Lista para mostrar en la tabla
  terminoBusqueda: string = '';
  categoriaForm: FormGroup;
  esEdicion = false;
  categoriaSeleccionadaId: number | null = null;
  isLoading = false;
  errorMessage: string | null = null;

  constructor() {
    this.categoriaForm = this.fb.group({
      id: [null],
      nombre: ['', [Validators.required, Validators.maxLength(100)]],
      descripcion: ['', [Validators.maxLength(255)]]
      // estado: ['ACTIVO'] // Podríamos añadirlo si el backend lo maneja
    });
  }

  ngOnInit() {
    this.listarCategorias();
  }

  listarCategorias() {
    this.isLoading = true;
    this.errorMessage = null;
    this.categoriaService.listarCategorias().subscribe({
      next: (data) => {
        this.categorias = data;
        // Inicialmente mostramos todas (o solo activas si el backend filtra)
        this.filtrarCategorias();
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error('Error al listar categorías:', err);
        this.errorMessage = 'Error al cargar categorías. Intente más tarde.';
        mostrarToast('Error al cargar categorías', 'danger');
        this.isLoading = false;
      },
    });
  }

  buscarCategoria() {
    this.filtrarCategorias();
  }

  filtrarCategorias() {
    const termino = this.terminoBusqueda.toLowerCase().trim();
    this.categoriasFiltradas = this.categorias.filter((cat) => {
      // Aquí podrías añadir filtro por estado si el backend lo devuelve
      // const estaActiva = cat.estado === 'ACTIVO';
      const coincideNombre = cat.nombre.toLowerCase().includes(termino);
      // return estaActiva && coincideNombre;
      return coincideNombre; // Filtro simple por nombre por ahora
    });
  }

  reiniciarFiltros() {
    this.terminoBusqueda = '';
    this.filtrarCategorias();
    mostrarToast('Filtros reiniciados');
  }

  abrirModalNuevo() {
    this.esEdicion = false;
    this.categoriaSeleccionadaId = null;
    this.categoriaForm.reset({
      id: null,
      nombre: '',
      descripcion: ''
      // estado: 'ACTIVO'
    });
    const modal = new bootstrap.Modal(document.getElementById('categoriaModal'));
    modal.show();
  }

  abrirModalEditar(categoria: CategoriaDTO) {
    if (!categoria.id) return; // Seguridad
    this.esEdicion = true;
    this.categoriaSeleccionadaId = categoria.id;
    this.categoriaForm.patchValue({
      id: categoria.id,
      nombre: categoria.nombre,
      descripcion: categoria.descripcion
      // estado: categoria.estado
    });
    const modal = new bootstrap.Modal(document.getElementById('categoriaModal'));
    modal.show();
  }

  guardarCategoria() {
    this.categoriaForm.markAllAsTouched();
    if (this.categoriaForm.invalid) {
      return;
    }

    this.isLoading = true; // Mostrar spinner
    this.errorMessage = null;
    const categoriaData = this.categoriaForm.value as CategoriaDTO;

    // Lógica para decidir si crear o actualizar
    const obs = this.esEdicion
      ? this.categoriaService.actualizar(categoriaData) // El DTO ya tiene el ID
      : this.categoriaService.crear(categoriaData);

    obs.subscribe({
      next: (categoriaGuardada) => {
        // Recargamos la lista completa para reflejar el cambio
        this.listarCategorias();
        mostrarToast(this.esEdicion ? 'Categoría actualizada' : 'Categoría creada', 'success');
        this.cerrarModal();
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error('Error al guardar categoría:', err);
        this.errorMessage = err.error?.message || 'Error al guardar la categoría.';
        if (this.errorMessage) { 
        mostrarToast(this.errorMessage, 'danger'); 
    }
        this.isLoading = false;
      },
    });
  }

  cerrarModal() {
    const modalElement = document.getElementById('categoriaModal');
    if (modalElement) {
      const modal = bootstrap.Modal.getInstance(modalElement);
      if (modal) {
        modal.hide();
      }
    }
  }

  eliminarCategoria(id?: number) {
    if (!id) return;
    if (confirm('¿Estás seguro de marcar esta categoría como inactiva?')) {
      this.isLoading = true;
      this.errorMessage = null;
      this.categoriaService.softDelete(id).subscribe({
        next: () => {
          // Recargamos la lista para que desaparezca (si el backend filtra)
          this.listarCategorias();
          mostrarToast('Categoría marcada como inactiva', 'warning');
          // No necesitamos ocultar isLoading aquí si listarCategorias lo hace
        },
        error: (err: any) => {
          console.error('Error al eliminar categoría:', err);
          this.errorMessage = err.error?.message || 'Error al eliminar categoría.';
          if (this.errorMessage) { 
        mostrarToast(this.errorMessage, 'danger'); 
    }
          this.isLoading = false;
        },
      });
    }
  }

  reactivarCategoria(id?: number) {
     if (!id) return;
     this.isLoading = true;
     this.errorMessage = null;
     this.categoriaService.reactivar(id).subscribe({
       next: () => {
         // Recargamos la lista para que reaparezca o cambie estado
         this.listarCategorias();
         mostrarToast('Categoría reactivada correctamente', 'success');
         // No necesitamos ocultar isLoading aquí si listarCategorias lo hace
       },
       error: (err: any) => {
         console.error('Error al reactivar categoría:', err);
         this.errorMessage = err.error?.message || 'Error al reactivar categoría.';
         if (this.errorMessage) { 
        mostrarToast(this.errorMessage, 'danger'); 
    }
         this.isLoading = false;
       },
     });
  }

  // Helper para validación en template
  get f() { return this.categoriaForm.controls; }
}