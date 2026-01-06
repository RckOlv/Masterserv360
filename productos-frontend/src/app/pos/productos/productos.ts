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
import { Page } from '../../models/page.model'; 
import { HasPermissionDirective } from '../../directives/has-permission.directive';

// Necesario para el modal de ajuste de stock
declare var bootstrap: any;

@Component({
  selector: 'app-productos',
  standalone: true,
  imports: [
    CommonModule,
    HttpClientModule,
    ReactiveFormsModule,
    FormsModule, 
    HasPermissionDirective,
    RouterLink // Importante para la navegación
  ],
  templateUrl: './productos.html',
  styleUrls: ['./productos.css']
})
export default class ProductosComponent implements OnInit {

  private productoService = inject(ProductoService);
  private categoriaService = inject(CategoriaService);
  private fb = inject(FormBuilder);

  // --- Paginación y Datos ---
  public productosPage: Page<ProductoDTO> | null = null;
  public categorias: CategoriaDTO[] = [];
  public filtroForm: FormGroup;
  
  public currentPage = 0;
  public pageSize = 10;
  
  // --- Estado UI ---
  public errorMessage: string | null = null;
  public isLoading = false;
  public isLoadingCategorias = false;

  // --- AJUSTE DE STOCK (MODAL) ---
  public ajusteForm: FormGroup;
  public productoAjustar: ProductoDTO | null = null;
  public isSavingAjuste = false;
  private modalAjusteInstance: any;

  constructor() {
    // Formulario de Filtros
    this.filtroForm = this.fb.group({
      nombre: [''],
      codigo: [''],
      categoriaId: [null],
      estado: [null],
      estadoStock: ['TODOS'] 
    });

    // Formulario para Ajuste Manual de Stock
    this.ajusteForm = this.fb.group({
        cantidad: [0, [Validators.required]], 
        motivo: ['', [Validators.required, Validators.minLength(5)]]
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
      error: (err) => {
        console.error(err);
        this.errorMessage = 'Error al cargar filtros de categorías.';
        this.isLoadingCategorias = false;
      }
    });
  }

  cargarProductos(): void {
    this.isLoading = true;
    this.errorMessage = null;

    const formValues = this.filtroForm.value;
    
    const filtro: ProductoFiltroDTO = {
        nombre: formValues.nombre,
        codigo: formValues.codigo,
        categoriaId: formValues.categoriaId,
        estado: formValues.estado,
        estadoStock: formValues.estadoStock,
        conStock: null, 
        precioMax: null
    };

    this.productoService.filtrarProductos(filtro, this.currentPage, this.pageSize).subscribe({
      next: (page) => {
        this.productosPage = page;
        this.isLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.handleError(err, 'cargar');
        this.isLoading = false;
      }
    });
  }

  // --- FILTROS Y PAGINACIÓN ---

  aplicarFiltros(): void {
    this.currentPage = 0; 
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
  
  get paginas(): number[] {
      if (!this.productosPage) return [];
      const total = this.productosPage.totalPages;
      const current = this.productosPage.number;
      const delta = 2; 
      const range = [];
      for (let i = Math.max(0, current - delta); i <= Math.min(total - 1, current + delta); i++) {
          range.push(i);
      }
      return range;
  }

  // ===========================
  //   MÉTODOS AJUSTE STOCK (MODAL)
  // ===========================

  abrirModalAjuste(producto: ProductoDTO) {
    this.productoAjustar = producto;
    this.ajusteForm.reset({ cantidad: 0, motivo: '' });
    
    const modalEl = document.getElementById('modalAjusteStock');
    if (modalEl) {
      this.modalAjusteInstance = new bootstrap.Modal(modalEl);
      this.modalAjusteInstance.show();
    }
  }

  confirmarAjuste() {
    if (this.ajusteForm.invalid || !this.productoAjustar?.id) {
        this.ajusteForm.markAllAsTouched();
        return;
    }

    const cantidad = this.ajusteForm.get('cantidad')?.value;
    const motivo = this.ajusteForm.get('motivo')?.value;

    if (cantidad === 0) {
        mostrarToast('La cantidad debe ser distinta de 0.', 'warning');
        return;
    }

    this.isSavingAjuste = true;

    // YA NO NECESITAMOS ENVIAR usuarioId NI tipoMovimiento
    // El backend se encarga de eso por seguridad.
    this.productoService.ajustarStock({
        productoId: this.productoAjustar.id!, 
        cantidad: cantidad,
        motivo: motivo
    }).subscribe({
        next: () => {
            mostrarToast('Stock ajustado correctamente.', 'success');
            if (this.modalAjusteInstance) this.modalAjusteInstance.hide();
            this.isSavingAjuste = false;
            this.cargarProductos(); 
        },
        error: (err) => {
            console.error(err);
            mostrarToast('Error al ajustar stock.', 'danger');
            this.isSavingAjuste = false;
        }
    });
  }

  // ===========================
  //   OTROS MÉTODOS
  // ===========================

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
    mostrarToast(this.errorMessage!, 'danger');
  }
}