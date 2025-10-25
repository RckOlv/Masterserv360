import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CategoriaService } from '../../service/categoria.service';
import { CategoriaDTO } from '../../models/categoria.model';
import { mostrarToast } from '../../utils/toast';

declare var bootstrap: any;

@Component({
  selector: 'app-categorias',
  standalone: true,
  templateUrl: './categorias.html',
  styleUrls: ['./categorias.css'],
  imports: [CommonModule, ReactiveFormsModule, FormsModule], 
})
export default class CategoriasComponent implements OnInit {

  private fb = inject(FormBuilder);
  private categoriaService = inject(CategoriaService);

  // Estado
  categorias: CategoriaDTO[] = []; // Lista completa del backend (filtrada por estado)
  categoriasFiltradas: CategoriaDTO[] = []; // Lista para la tabla (filtrada por nombre)
  terminoBusqueda: string = '';
  filtroEstado: string = 'ACTIVO'; // Estado por defecto
  
  categoriaForm: FormGroup;
  esEdicion = false;
  categoriaSeleccionadaId: number | null = null;
  isLoading = false;
  errorMessage: string | null = null;

  constructor() {
    this.categoriaForm = this.fb.group({
      id: [null],
      nombre: ['', [Validators.required, Validators.maxLength(100)]],
      descripcion: ['', [Validators.maxLength(255)]],
      estado: ['ACTIVO'] 
    });
  }

  ngOnInit() {
    this.listarCategorias(); // Carga inicial (solo Activos)
  }

  /** 🔹 Obtener categorías del backend (filtradas por estado) */
  listarCategorias() {
    this.isLoading = true;
    this.errorMessage = null;
    
    // 1. Llama al backend CON el filtro de estado
    this.categoriaService.listarCategorias(this.filtroEstado).subscribe({
      next: (data: CategoriaDTO[]) => {
        this.categorias = data; // Guarda la lista filtrada por estado
        // 2. Ahora aplica el filtro de nombre localmente
        this.filtrarLocalmentePorNombre(); 
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error('Error al listar categorías:', err);
        this.errorMessage = 'Error al cargar categorías. Intente más tarde.';
        if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
        this.isLoading = false;
      },
    });
  }

  /** 🔹 Se llama CADA VEZ que un filtro cambia */
  aplicarFiltros(): void {
    // Si el cambio fue en el input de nombre, solo filtramos localmente
    if (this.filtroEstado === 'ACTIVO' && this.categorias.length > 0) {
       // Optimización: si solo busca por nombre, no recargar todo
       // (Podemos refinar esto, pero por ahora recargamos siempre)
    }
    
    // La forma más simple: CUALQUIER cambio de filtro, recarga del backend
    this.listarCategorias();
  }

  /** 🔹 Filtra la lista 'categorias' (ya filtrada por estado) por el término de búsqueda */
  filtrarLocalmentePorNombre() {
    const termino = this.terminoBusqueda.toLowerCase().trim();
    if (!termino) {
      this.categoriasFiltradas = [...this.categorias]; // Sin filtro, copia la lista
    } else {
      this.categoriasFiltradas = this.categorias.filter((cat) => 
        cat.nombre.toLowerCase().includes(termino)
      );
    }
  }

  /** 🔹 Reiniciar filtros */
  reiniciarFiltros() {
    this.terminoBusqueda = '';
    this.filtroEstado = 'ACTIVO'; // Vuelve al estado por defecto
    this.listarCategorias(); // Recarga
    mostrarToast('Filtros reiniciados');
  }

  // ... (El resto de tus métodos: abrirModalNuevo, abrirModalEditar, guardarCategoria, etc. están bien)
  // Asegúrate de que los métodos de guardar, eliminar y reactivar sigan llamando a this.listarCategorias()
  // al finalizar con éxito, para refrescar la tabla.

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
    if (this.categoriaForm.invalid) return;

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
        // isLoading se resetea en listarCategorias()
      },
      error: (err: any) => {
        console.error('Error al guardar categoría:', err);
        this.errorMessage = err.error?.message || 'Error al guardar la categoría.';
        if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
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

  eliminarCategoria(id?: number) {
    if (!id) return;
    if (confirm('¿Estás seguro de marcar esta categoría como INACTIVA?')) {
      this.isLoading = true;
      this.errorMessage = null;
      this.categoriaService.softDelete(id).subscribe({
        next: () => {
          this.listarCategorias(); 
          mostrarToast('Categoría marcada como inactiva', 'warning');
        },
        error: (err: any) => {
          console.error('Error al eliminar categoría:', err);
          this.errorMessage = err.error?.message || 'Error al eliminar categoría.';
          if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
          this.isLoading = false;
        },
      });
    }
  }

  reactivarCategoria(id?: number) {
     if (!id) return;
     if (confirm('¿Estás seguro de REACTIVAR esta categoría?')) {
       this.isLoading = true;
       this.errorMessage = null;
       this.categoriaService.reactivar(id).subscribe({
         next: () => {
           this.listarCategorias(); 
           mostrarToast('Categoría reactivada correctamente', 'success');
         },
         error: (err: any) => {
           console.error('Error al reactivar categoría:', err);
           this.errorMessage = err.error?.message || 'Error al reactivar categoría.';
           if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
           this.isLoading = false;
         },
       });
     }
  }

  get f() { return this.categoriaForm.controls; }
}