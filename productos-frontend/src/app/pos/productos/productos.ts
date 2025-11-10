import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ProductoService, Page } from '../../service/producto.service';
import { CategoriaService } from '../../service/categoria.service';
import { ProductoDTO } from '../../models/producto.model';
import { ProductoFiltroDTO } from '../../models/producto-filtro.model';
import { CategoriaDTO } from '../../models/categoria.model';

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
  private categoriaService = inject(CategoriaService);
  private fb = inject(FormBuilder);

  // Estado del componente
  public productosPage: Page<ProductoDTO> | null = null;
  public categorias: CategoriaDTO[] = [];
  public filtroForm: FormGroup;
  public currentPage = 0;
  public pageSize = 10;
  public errorMessage: string | null = null;
  public isLoading = false;
  public isLoadingCategorias = false;

  // Nuevas propiedades para el modal
  public mostrarModal = false;
  public modalTitle = 'Nuevo Producto';
  public productoEditando: ProductoDTO | null = null;
  public isSubmitting = false;
  public modalErrorMessage: string | null = null;

  // Formulario para el modal
  public productoForm: FormGroup;

  constructor() {
    // Inicializamos el formulario de filtros
    this.filtroForm = this.fb.group({
      nombre: [''],
      codigo: [''],
      categoriaId: [null],
      estado:[null],
      conStock: [null]
    });

    // Inicializamos el formulario del producto
    this.productoForm = this.fb.group({
      codigo: ['', [Validators.required, Validators.maxLength(50)]],
      nombre: ['', [Validators.required, Validators.maxLength(255)]],
      descripcion: [''],
      precioVenta: [0, [Validators.required, Validators.min(0)]],
      precioCosto: [0, [Validators.required, Validators.min(0)]],
      stockMinimo: [0, [Validators.required, Validators.min(0)]],
      categoriaId: [null, [Validators.required]],
      estado: ['ACTIVO', [Validators.required]],
      imagenUrl: ['', [Validators.maxLength(255)]]
    });
  }

  ngOnInit(): void {
    this.cargarCategorias();
    this.cargarProductos();
  }

  /**
   * Carga las categorías desde el servicio
   */
  cargarCategorias(): void {
    this.isLoadingCategorias = true;
    
    this.categoriaService.listarCategorias('ACTIVO').subscribe({
      next: (categorias) => {
        this.categorias = categorias;
        this.isLoadingCategorias = false;
      },
      error: (err) => {
        console.error('Error al cargar categorías:', err);
        this.isLoadingCategorias = false;
      }
    });
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
        console.log('Datos de paginación:', page);
      },
      error: (err) => {
        console.error('Error al cargar productos:', err);
        this.errorMessage = 'Error al cargar productos. Intente más tarde.';
        this.isLoading = false;
      }
    });
  }

  /**
   * MÉTODOS DEL MODAL
   */
  abrirModalNuevoProducto(): void {
    this.modalTitle = 'Nuevo Producto';
    this.productoEditando = null;
    this.modalErrorMessage = null;
    this.productoForm.reset({
      codigo: '',
      nombre: '',
      descripcion: '',
      precioVenta: 0,
      precioCosto: 0,
      stockMinimo: 0,
      categoriaId: null,
      estado: 'ACTIVO',
      imagenUrl: ''
    });
    this.mostrarModal = true;
  }

  abrirModalEditarProducto(producto: ProductoDTO): void {
    this.modalTitle = 'Editar Producto';
    this.productoEditando = producto;
    this.modalErrorMessage = null;
    
    this.productoForm.patchValue({
      codigo: producto.codigo,
      nombre: producto.nombre,
      descripcion: producto.descripcion || '',
      precioVenta: producto.precioVenta,
      precioCosto: producto.precioCosto,
      stockMinimo: producto.stockMinimo,
      categoriaId: producto.categoriaId,
      estado: producto.estado,
      imagenUrl: producto.imagenUrl || ''
    });
    
    this.mostrarModal = true;
  }

  cerrarModal(): void {
    this.mostrarModal = false;
    this.productoEditando = null;
    this.modalErrorMessage = null;
    this.isSubmitting = false;
  }

  onSubmitModal(): void {
    if (this.productoForm.invalid) {
      this.markFormGroupTouched(this.productoForm);
      return;
    }

    this.isSubmitting = true;
    this.modalErrorMessage = null;

    const productoData = this.productoForm.value;

    if (this.productoEditando) {
      // Editar producto existente
      this.productoService.actualizarProducto(this.productoEditando.id!, productoData)
        .subscribe({
          next: () => {
            this.cerrarModal();
            this.cargarProductos();
          },
          error: (err) => {
            console.error('Error al actualizar producto:', err);
            this.modalErrorMessage = err.error?.message || 'Error al actualizar el producto.';
            this.isSubmitting = false;
          }
        });
    } else {
      // Crear nuevo producto
      this.productoService.crearProducto(productoData)
        .subscribe({
          next: () => {
            this.cerrarModal();
            this.cargarProductos();
          },
          error: (err) => {
            console.error('Error al crear producto:', err);
            this.modalErrorMessage = err.error?.message || 'Error al crear el producto.';
            this.isSubmitting = false;
          }
        });
    }
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }

  /**
   * Método para eliminar un producto.
   */
  eliminarProducto(id: number | undefined): void {
    if (!id) {
      console.error('Intento de eliminar producto sin ID.');
      return;
    }

    if (confirm(`¿Estás seguro de que deseas eliminar este producto?`)) {
      this.isLoading = true;
      this.errorMessage = null;

      this.productoService.eliminarProducto(id).subscribe({
        next: () => {
          alert('Producto eliminado exitosamente.');
          this.cargarProductos();
        },
        error: (err) => {
          console.error('Error al eliminar producto:', err);
          this.errorMessage = err.error?.message || 'Error al eliminar el producto. Intente más tarde.';
          this.isLoading = false;
        }
      });
    }
  }

  /**
   * Se llama cuando se envía el formulario de filtros
   */
  aplicarFiltros(): void {
    this.currentPage = 0;
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

  // --- Métodos de Paginación Mejorados ---
  
  irAPagina(pageNumber: number): void {
    if (pageNumber >= 0 && pageNumber < this.totalPaginas) {
      this.currentPage = pageNumber;
      this.cargarProductos();
    }
  }

  get totalPaginas(): number {
    return this.productosPage ? this.productosPage.totalPages : 0;
  }

  get paginas(): number[] {
    if (!this.productosPage) return [];
    
    const pages = [];
    const totalPages = this.productosPage.totalPages;
    const currentPage = this.productosPage.number;
    
    // Mostrar máximo 5 páginas alrededor de la actual
    let startPage = Math.max(0, currentPage - 2);
    let endPage = Math.min(totalPages - 1, currentPage + 2);
    
    // Ajustar si estamos cerca del inicio
    if (currentPage < 3) {
      endPage = Math.min(4, totalPages - 1);
    }
    
    // Ajustar si estamos cerca del final
    if (currentPage > totalPages - 4) {
      startPage = Math.max(0, totalPages - 5);
    }
    
    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }
    
    return pages;
  }

  // Helper para el formulario
  get f() {
    return this.productoForm.controls;
  }

  get filtroF() {
    return this.filtroForm.controls;
  }
}