import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClientModule, HttpErrorResponse } from '@angular/common/http';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ProductoService } from '../../service/producto.service'; 
import { CategoriaService } from '../../service/categoria.service';
import { AuthService } from '../../service/auth.service';
import { ProductoDTO } from '../../models/producto.model';
import { ProductoFiltroDTO } from '../../models/producto-filtro.model';
import { CategoriaDTO } from '../../models/categoria.model';
import { mostrarToast } from '../../utils/toast';

// IMPORTANTE: Importar la interfaz Page desde el modelo compartido
import { Page } from '../../models/page.model'; 

import { HasPermissionDirective } from '../../directives/has-permission.directive';

@Component({
  selector: 'app-productos',
  standalone: true,
  imports: [
    CommonModule,
    HttpClientModule,
    ReactiveFormsModule,
    FormsModule, // Necesario si usas ngModel en algún lado, aunque aquí es todo reactive
    HasPermissionDirective,
    RouterLink
  ],
  templateUrl: './productos.html',
  styleUrls: ['./productos.css']
})
export default class ProductosComponent implements OnInit {

  private productoService = inject(ProductoService);
  private categoriaService = inject(CategoriaService);
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);

  // --- Paginación y Datos ---
  // Inicializamos como null, y se llenará con la respuesta del backend
  public productosPage: Page<ProductoDTO> | null = null;
  public categorias: CategoriaDTO[] = [];
  public filtroForm: FormGroup;
  
  // Variables de control de paginación
  public currentPage = 0;
  public pageSize = 10;
  
  // --- Estado UI ---
  public errorMessage: string | null = null;
  public isLoading = false;
  public isLoadingCategorias = false;

  // --- Modal ---
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
      // Filtro unificado de stock
      estadoStock: ['TODOS'] 
    });

    this.productoForm = this.fb.group({
      codigo: ['', [Validators.required, Validators.maxLength(50)]],
      nombre: ['', [Validators.required, Validators.maxLength(255)]],
      descripcion: [''],
      precioVenta: [0, [Validators.required, Validators.min(0)]],
      precioCosto: [0, [Validators.required, Validators.min(0)]],
      stockMinimo: [0, [Validators.required, Validators.min(0)]],
      loteReposicion: [0, [Validators.required, Validators.min(0)]],
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

    const formValues = this.filtroForm.value;
    
    // Mapeo al DTO del backend
    const filtro: ProductoFiltroDTO = {
        nombre: formValues.nombre,
        codigo: formValues.codigo,
        categoriaId: formValues.categoriaId,
        estado: formValues.estado,
        estadoStock: formValues.estadoStock,
        conStock: null, 
        precioMax: null
    };

    // Llamada al servicio con paginación
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

  // --- MÉTODOS DE FILTRO Y PAGINACIÓN ---

  aplicarFiltros(): void {
    this.currentPage = 0; // Al filtrar, siempre volvemos a la primera página
    this.cargarProductos();
  }

  limpiarFiltros(): void {
    this.filtroForm.reset({
      nombre: '',
      codigo: '',
      categoriaId: null,
      estado: null,
      estadoStock: 'TODOS'
    });
    this.aplicarFiltros();
  }

  paginaAnterior(): void {
    if (this.productosPage && !this.productosPage.first) {
      this.currentPage--;
      this.cargarProductos();
    }
  }

  paginaSiguiente(): void {
    if (this.productosPage && !this.productosPage.last) {
      this.currentPage++;
      this.cargarProductos();
    }
  }

  irAPagina(pageNumber: number): void {
    if (this.productosPage && pageNumber >= 0 && pageNumber < this.productosPage.totalPages) {
        this.currentPage = pageNumber;
        this.cargarProductos();
    }
  }
  
  // Getter para generar el array de números de página [0, 1, 2...]
  get paginas(): number[] {
      if (!this.productosPage) return [];
      const total = this.productosPage.totalPages;
      const current = this.productosPage.number;
      const delta = 2; // Cuántas páginas mostrar a los lados de la actual
      const range = [];
      for (let i = Math.max(0, current - delta); i <= Math.min(total - 1, current + delta); i++) {
          range.push(i);
      }
      return range;
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
      loteReposicion: 0,
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
      loteReposicion: producto.loteReposicion,
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

  private handleError(err: HttpErrorResponse, context: string) {
    if (err.status === 403) {
      this.errorMessage = 'Acción no permitida: No tiene permisos.';
    } else if (err.status === 500) {
      this.errorMessage = 'Error interno en el servidor.';
    } else {
      this.errorMessage = err.error?.message || `Error al ${context} el producto.`;
    }
    // Solo mostramos toast si no estamos en el modal (el modal tiene su propio mensaje)
    if (this.errorMessage && !this.mostrarModal) mostrarToast(this.errorMessage, 'danger');
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
}