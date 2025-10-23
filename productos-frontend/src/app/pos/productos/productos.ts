import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common'; // Para *ngFor, *ngIf
import { HttpClientModule } from '@angular/common/http';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms'; // Para filtros
import { RouterLink } from '@angular/router'; // Para botones de acción

import { ProductoService, Page } from '../../service/producto.service'; // Ajusta ruta
import { ProductoDTO } from '../../models/producto.model';
import { ProductoFiltroDTO } from '../../models/producto-filtro.model';

@Component({
  selector: 'app-productos',
  standalone: true,
  imports: [
    CommonModule,
    HttpClientModule,
    ReactiveFormsModule,
    RouterLink
  ],
  templateUrl: './productos.html',
  styleUrls: ['./productos.css']
})
export default class ProductosComponent implements OnInit {

  // Inyección de dependencias
  private productoService = inject(ProductoService);
  private fb = inject(FormBuilder);

  // Estado del componente
  public productosPage: Page<ProductoDTO> | null = null;
  public filtroForm: FormGroup;
  public currentPage = 0;
  public pageSize = 10;
  public errorMessage: string | null = null;
  public isLoading = false;

  constructor() {
    // Inicializamos el formulario de filtros
    this.filtroForm = this.fb.group({
      nombre: [''],
      codigo: [''],
      categoriaId: [null],
      // precioMax: [null], // Añadir si necesitas
      conStock: [null] // null = Todos, true = Con stock, false = Sin stock
    });
  }

  ngOnInit(): void {
    this.cargarProductos(); // Carga inicial
  }

  /**
   * Llama al servicio para cargar/filtrar productos
   */
  cargarProductos(): void {
    this.isLoading = true;
    this.errorMessage = null;
    const filtro = this.filtroForm.value as ProductoFiltroDTO;

    this.productoService.filtrarProductos(filtro, this.currentPage, this.pageSize).subscribe({
      next: (page) => {
        this.productosPage = page;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error al cargar productos:', err);
        this.errorMessage = 'Error al cargar productos. Intente más tarde.';
        this.isLoading = false;
      }
    });
  }

  /**
   * Método para eliminar un producto.
   * Se llama desde el botón en la tabla.
   */
  eliminarProducto(id: number | undefined): void {
    // Verificamos que el ID exista (buena práctica)
    if (!id) {
      console.error('Intento de eliminar producto sin ID.');
      return;
    }

    // Confirmación (¡Siempre confirma antes de borrar!)
    if (confirm(`¿Estás seguro de que deseas eliminar el producto con ID ${id}?`)) {
      this.isLoading = true; // Mostramos spinner mientras borra
      this.errorMessage = null;

      this.productoService.eliminarProducto(id).subscribe({
        next: () => {
          // Éxito: Mostramos mensaje y recargamos la lista
          alert(`Producto con ID ${id} eliminado exitosamente.`);
          this.cargarProductos(); // Recarga la página actual
        },
        error: (err) => {
          // Error: Mostramos mensaje
          console.error('Error al eliminar producto:', err);
          this.errorMessage = err.error?.message || 'Error al eliminar el producto. Intente más tarde.';
          this.isLoading = false; // Ocultamos spinner en caso de error
        }
      });
    }
  }

  /**
   * Se llama cuando se envía el formulario de filtros
   */
  aplicarFiltros(): void {
    this.currentPage = 0; // Resetea a la primera página con nuevos filtros
    this.cargarProductos();
  }

  /**
   * Resetea los filtros y recarga la lista
   */
  limpiarFiltros(): void {
    this.filtroForm.reset({
      nombre: '',
      codigo: '',
      categoriaId: null,
      conStock: null
    });
    this.aplicarFiltros();
  }

  // --- Métodos de Paginación ---
  
  irAPagina(pageNumber: number): void {
    if (pageNumber >= 0 && (!this.productosPage || pageNumber < this.productosPage.totalPages)) {
      this.currentPage = pageNumber;
      this.cargarProductos();
    }
  }

  get totalPaginas(): number {
    return this.productosPage ? this.productosPage.totalPages : 0;
  }
}