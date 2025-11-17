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

import { HasPermissionDirective } from '../../directives/has-permission.directive';

@Component({
  selector: 'app-productos',
  standalone: true,
  imports: [
    CommonModule,
    HttpClientModule,
    ReactiveFormsModule,
    HasPermissionDirective
  ],
  templateUrl: './productos.html',
  styleUrls: ['./productos.css']
})
export default class ProductosComponent implements OnInit {

  private productoService = inject(ProductoService);
  private categoriaService = inject(CategoriaService);
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);

  public productosPage: Page<ProductoDTO> | null = null;
  public categorias: CategoriaDTO[] = [];
  public filtroForm: FormGroup;
  public currentPage = 0;
  public pageSize = 10;
  public errorMessage: string | null = null;
  public isLoading = false;
  public isLoadingCategorias = false;

  public mostrarModal = false;
  public modalTitle = 'Nuevo Producto';
  public productoEditando: ProductoDTO | null = null;
  public isSubmitting = false;
  public modalErrorMessage: string | null = null;

  public productoForm: FormGroup;

  constructor() {

    this.filtroForm = this.fb.group({
      nombre: [''],
      codigo: [''],
      categoriaId: [null],
      estado: [null],
      conStock: [null]
    });

    this.productoForm = this.fb.group({
      codigo: ['', [Validators.required, Validators.maxLength(50)]],
      nombre: ['', [Validators.required, Validators.maxLength(255)]],
      descripcion: [''],
      precioVenta: [0, [Validators.required, Validators.min(0)]],
      precioCosto: [0, [Validators.required, Validators.min(0)]],
      stockMinimo: [0, [Validators.required, Validators.min(0)]],
      loteReposicion: [0, [Validators.required, Validators.min(0)]],   // 👈 AÑADIDO
      categoriaId: [null, [Validators.required]],
      estado: ['ACTIVO', [Validators.required]],
      imagenUrl: ['', [Validators.maxLength(255)]]
    });
  }

  ngOnInit(): void {
    this.cargarCategorias();
    this.cargarProductos();
  }

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

  cargarProductos(): void {
    this.isLoading = true;
    this.errorMessage = null;

    const filtro = this.filtroForm.value as ProductoFiltroDTO;

    this.productoService.filtrarProductos(filtro, this.currentPage, this.pageSize).subscribe({
      next: (page) => {
        this.productosPage = page;
        this.isLoading = false;
      },
      error: (err: HttpErrorResponse) => {
        console.error('Error al cargar productos:', err);
        this.handleError(err, 'cargar');
        this.isLoading = false;
      }
    });
  }

  // ===========================
  //   MÉTODOS DEL MODAL
  // ===========================

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
      loteReposicion: 0,       // 👈 AÑADIDO
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
      loteReposicion: producto.loteReposicion,   // 👈 AÑADIDO
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

  eliminarProducto(id: number | undefined): void {
    if (!id) return;

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

  aplicarFiltros(): void {
    this.currentPage = 0;
    this.cargarProductos();
  }

  limpiarFiltros(): void {
    this.filtroForm.reset({
      nombre: '',
      codigo: '',
      categoriaId: null,
      conStock: null
    });
    this.aplicarFiltros();
  }

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
      this.errorMessage = 'Acción no permitida: No tiene permisos.';
    } else if (err.status === 500) {
      this.errorMessage = 'Error interno en el servidor.';
    } else {
      this.errorMessage = err.error?.message || `Error al ${context} el producto.`;
    }
    if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }

  get f() {
    return this.productoForm.controls;
  }

  get filtroF() {
    return this.filtroForm.controls;
  }
}
