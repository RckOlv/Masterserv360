import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClientModule, HttpErrorResponse } from '@angular/common/http';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';

import { ProductoService, Page } from '../../service/producto.service';
import { CategoriaService } from '../../service/categoria.service';
import { AuthService } from '../../service/auth.service';
import { ProductoDTO } from '../../models/producto.model';
import { ProductoFiltroDTO } from '../../models/producto-filtro.model';
import { CategoriaDTO } from '../../models/categoria.model';
import { mostrarToast } from '../../utils/toast';

// --- Mentor: INICIO DE LA MODIFICACIÓN ---
// 1. Importar la nueva directiva de permisos
import { HasPermissionDirective } from '../../directives/has-permission.directive';
// --- Mentor: FIN DE LA MODIFICACIÓN ---

@Component({
  selector: 'app-productos',
  standalone: true,
  imports: [
    CommonModule,
    HttpClientModule,
    ReactiveFormsModule,
    // --- Mentor: INICIO DE LA MODIFICACIÓN ---
    // 2. Añadir la directiva a los imports del componente
    HasPermissionDirective
    // --- Mentor: FIN DE LA MODIFICACIÓN ---
  ],
  templateUrl: './productos.html',
  styleUrls: ['./productos.css']
})
export default class ProductosComponent implements OnInit {

  // Inyección de dependencias
  private productoService = inject(ProductoService);
  private categoriaService = inject(CategoriaService);
  private fb = inject(FormBuilder);
  // AuthService todavía se inyecta, pero ya no lo usamos para 'isAdmin'
  private authService = inject(AuthService); 

  // Estado del componente
  public productosPage: Page<ProductoDTO> | null = null;
  public categorias: CategoriaDTO[] = [];
  public filtroForm: FormGroup;
  public currentPage = 0;
  public pageSize = 10;
  public errorMessage: string | null = null;
  public isLoading = false;
  public isLoadingCategorias = false;

  // --- Mentor: INICIO DE LA MODIFICACIÓN ---
  // 3. 'isAdmin' ya no es necesaria aquí, la directiva lo maneja.
  // public isAdmin = false; 
  // --- Mentor: FIN DE LA MODIFICACIÓN ---

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
    // --- Mentor: INICIO DE LA MODIFICACIÓN ---
    // 4. Esta línea ya no es necesaria.
    // this.isAdmin = this.authService.hasRole('ROLE_ADMIN');
    // --- Mentor: FIN DE LA MODIFICACIÓN ---

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
      error: (err: HttpErrorResponse) => {
        console.error('Error al cargar categorías:', err);
        this.errorMessage = 'Error al cargar filtros de categorías.';
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
      error: (err: HttpErrorResponse) => {
        console.error('Error al cargar productos:', err);
        this.handleError(err, 'cargar'); 
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
            mostrarToast('Producto actualizado con éxito.', 'success');
            this.cerrarModal();
            this.cargarProductos();
          },
          error: (err: HttpErrorResponse) => {
            console.error('Error al actualizar producto:', err);
            this.handleError(err, 'actualizar (modal)');
            this.modalErrorMessage = this.errorMessage;
            this.isSubmitting = false;
          }
        });
    } else {
      // Crear nuevo producto
      this.productoService.crearProducto(productoData)
        .subscribe({
          next: () => {
            mostrarToast('Producto creado con éxito.', 'success');
            this.cerrarModal();
            this.cargarProductos();
          },
          error: (err: HttpErrorResponse) => {
            console.error('Error al crear producto:', err);
            this.handleError(err, 'crear (modal)');
            this.modalErrorMessage = this.errorMessage;
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
          mostrarToast('Producto eliminado exitosamente.', 'success');
          this.cargarProductos();
        },
        error: (err: HttpErrorResponse) => {
          console.error('Error al eliminar producto:', err);
          this.handleError(err, 'eliminar');
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
    
    let startPage = Math.max(0, currentPage - 2);
    let endPage = Math.min(totalPages - 1, currentPage + 2);
    
    if (currentPage < 3) {
      endPage = Math.min(4, totalPages - 1);
    }
    
    if (currentPage > totalPages - 4) {
      startPage = Math.max(0, totalPages - 5);
    }
    
    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }
    
    return pages;
  }

  private handleError(err: HttpErrorResponse, context: string) {
    if (err.status === 403) {
      this.errorMessage = 'Acción no permitida: No tiene permisos de Administrador.';
    } else if (err.status === 500) {
      this.errorMessage = 'Ocurrió un error interno en el servidor.';
    } else {
      this.errorMessage = err.error?.message || `Error al ${context} el producto.`;
    }
    if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
  }

  // Helper para el formulario
  get f() {
    return this.productoForm.controls;
  }

  get filtroF() {
    return this.filtroForm.controls;
  }
}