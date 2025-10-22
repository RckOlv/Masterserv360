import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CategoriaService } from '../../service/categoria.service';
import { Categoria } from '../../models/categoria.model';
import { mostrarToast } from '../../utils/toast';

@Component({
  selector: 'app-categorias',
  standalone: true,
  templateUrl: './categorias.html',
  styleUrls: ['./categorias.css'],
  imports: [CommonModule, FormsModule],
})
export class CategoriasComponent implements OnInit {
  categorias: Categoria[] = [];
  categoriasFiltradas: Categoria[] = [];
  mostrarModal = false;
  esEdicion = false;
  terminoBusqueda: string = '';
  filtroEstado: string = 'todas';
  nuevaCategoria: Categoria = { nombreCategoria: '', descripcion: '', activo: true };

  constructor(private categoriaService: CategoriaService) {}

  ngOnInit() {
    this.listarCategorias();
  }

  /** 🔹 Obtener todas las categorías del backend */
  listarCategorias() {
    this.categoriaService.listarCategorias().subscribe({
      next: (data) => {
        this.categorias = data;
        this.filtrarCategorias();
      },
      error: (err) => {
        console.error('Error al listar categorías:', err);
        mostrarToast('Error al cargar categorías', 'danger');
      },
    });
  }

  /** 🔹 Búsqueda instantánea */
  buscarCategoria() {
    this.filtrarCategorias();
  }

  /** 🔹 Filtra por nombre y estado */
  filtrarCategorias() {
    const termino = this.terminoBusqueda.toLowerCase().trim();
    const estado = this.filtroEstado;

    this.categoriasFiltradas = this.categorias.filter((cat) => {
      const coincideNombre = cat.nombreCategoria.toLowerCase().includes(termino);
      const coincideEstado =
        estado === 'todas'
          ? true
          : estado === 'activas'
          ? cat.activo
          : !cat.activo;

      return coincideNombre && coincideEstado;
    });
  }

  /** 🔹 Reiniciar filtros */
  reiniciarFiltros() {
    this.terminoBusqueda = '';
    this.filtroEstado = 'todas';
    this.filtrarCategorias();
    mostrarToast('Filtros reiniciados');
  }

  /** 🔹 Abrir o preparar el modal */
  toggleModal(categoria: Categoria | null = null) {
    this.esEdicion = !!categoria;
    this.nuevaCategoria = categoria
      ? { ...categoria }
      : { nombreCategoria: '', descripcion: '', activo: true };

    const modal = (window as any).bootstrap.Modal.getOrCreateInstance(
      document.getElementById('categoriaModal')
    );

    modal.show();
  }

  /** 🔹 Guardar o editar categoría */
  guardarCategoria() {
    if (!this.nuevaCategoria.nombreCategoria.trim()) {
      mostrarToast('El nombre es requerido', 'danger');
      return;
    }

    const obs = this.esEdicion
      ? this.categoriaService.actualizar(this.nuevaCategoria)
      : this.categoriaService.crear(this.nuevaCategoria);

    obs.subscribe({
      next: (categoriaGuardada) => {
        if (this.esEdicion) {
          const index = this.categorias.findIndex(
            (c) => c.idCategoria === categoriaGuardada.idCategoria
          );
          if (index !== -1) this.categorias[index] = categoriaGuardada;
          mostrarToast('Categoría actualizada correctamente');
        } else {
          this.categorias.push(categoriaGuardada);
          mostrarToast('Categoría creada correctamente');
        }

        this.filtrarCategorias();

        const modal = (window as any).bootstrap.Modal.getInstance(
          document.getElementById('categoriaModal')
        );
        modal.hide();
      },
      error: (err) => {
        console.error('Error al guardar categoría:', err);
        mostrarToast('Error al guardar categoría', 'danger');
      },
    });
  }

  /** 🔹 Eliminar categoría */
  eliminarCategoria(idCategoria?: number) {
    if (!idCategoria) return;
    if (confirm('¿Estás seguro de eliminar esta categoría?')) {
      this.categoriaService.darDebajaLogico(idCategoria).subscribe({
        next: () => {
          const cat = this.categorias.find((c) => c.idCategoria === idCategoria);
          if (cat) cat.activo = false;
          this.filtrarCategorias();
          mostrarToast('Categoría eliminada correctamente', 'warning');
        },
        error: (err) => {
          console.error('Error al eliminar categoría:', err);
          mostrarToast('Error al eliminar categoría', 'danger');
        },
      });
    }
  }

  /** 🔹 Reactivar categoría */
  reactivarCategoria(idCategoria?: number) {
    if (!idCategoria) return;
    this.categoriaService.reactivarCategoria(idCategoria).subscribe({
      next: () => {
        const cat = this.categorias.find((c) => c.idCategoria === idCategoria);
        if (cat) cat.activo = true;
        this.filtrarCategorias();
        console.log('Categoría reactivada:', idCategoria);
        mostrarToast('Categoría reactivada correctamente');
      },
      error: (err) => {
        console.error('Error al reactivar categoría:', err);
        mostrarToast('Error al reactivar categoría', 'danger');
      },
    });
  }
}
