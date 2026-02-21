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
import { mostrarToast, confirmarAccion } from '../../utils/toast'; // ✅ AÑADIDO confirmarAccion
import { Page } from '../../models/page.model'; 
import { HasPermissionDirective } from '../../directives/has-permission.directive';

// 📄 Librerías para PDF y Excel
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import * as XLSX from 'xlsx';

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
    RouterLink
  ],
  templateUrl: './productos.html',
  styleUrls: ['./productos.css']
})
export default class ProductosComponent implements OnInit {

  private productoService = inject(ProductoService);
  private categoriaService = inject(CategoriaService);
  private fb = inject(FormBuilder);

  public productosPage: Page<ProductoDTO> | null = null;
  public categorias: CategoriaDTO[] = [];
  public filtroForm: FormGroup;
  
  public currentPage = 0;
  public pageSize = 10;
  
  public errorMessage: string | null = null;
  public isLoading = false;
  public isLoadingCategorias = false;

  public ajusteForm: FormGroup;
  public productoAjustar: ProductoDTO | null = null;
  public isSavingAjuste = false;
  private modalAjusteInstance: any;

  constructor() {
    this.filtroForm = this.fb.group({
      nombre: [''],
      codigo: [''],
      categoriaId: [null],
      estado: [null],
      estadoStock: ['TODOS'] 
    });

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

  aplicarFiltros(): void {
    this.currentPage = 0; 
    this.cargarProductos();
  }

  limpiarFiltros(): void {
    this.filtroForm.reset({
      nombre: '', codigo: '', categoriaId: null, estado: null, estadoStock: 'TODOS'
    });
    this.aplicarFiltros();
  }

  paginaAnterior(): void {
    if (this.productosPage && !this.productosPage.first) { this.currentPage--; this.cargarProductos(); }
  }

  paginaSiguiente(): void {
    if (this.productosPage && !this.productosPage.last) { this.currentPage++; this.cargarProductos(); }
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
    if (this.ajusteForm.invalid || !this.productoAjustar?.id) { this.ajusteForm.markAllAsTouched(); return; }

    const cantidad = this.ajusteForm.get('cantidad')?.value;
    const motivo = this.ajusteForm.get('motivo')?.value;

    if (cantidad === 0) { mostrarToast('La cantidad debe ser distinta de 0.', 'warning'); return; }

    this.isSavingAjuste = true;

    this.productoService.ajustarStock({ productoId: this.productoAjustar.id!, cantidad, motivo }).subscribe({
        next: () => {
            mostrarToast('Stock ajustado correctamente.', 'success');
            if (this.modalAjusteInstance) this.modalAjusteInstance.hide();
            this.isSavingAjuste = false;
            this.cargarProductos(); 
        },
        error: (err) => { console.error(err); mostrarToast('Error al ajustar stock.', 'danger'); this.isSavingAjuste = false; }
    });
  }

  /** ✅ ELIMINAR PRODUCTO MIGRADO */
  eliminarProducto(id: number | undefined): void {
    if (!id) return;
    
    confirmarAccion(
      'Eliminar Producto',
      '¿Estás seguro de que deseas eliminar este producto? Esta acción no se puede deshacer.'
    ).then((confirmado) => {
      if (confirmado) {
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
    });
  }

  private handleError(err: HttpErrorResponse, context: string) {
    if (err.status === 403) this.errorMessage = 'Acción no permitida: No tiene permisos.';
    else if (err.status === 500) this.errorMessage = 'Error interno en el servidor.';
    else this.errorMessage = err.error?.message || `Error al ${context} el producto.`;
    mostrarToast(this.errorMessage!, 'danger');
  }

  // ==========================================
  // 📥 MÉTODOS DE EXPORTACIÓN (Catálogo)
  // ==========================================

  private obtenerTodosParaExportar(callback: (productos: ProductoDTO[]) => void) {
    this.isLoading = true;
    const formValues = this.filtroForm.value;
    const filtro: ProductoFiltroDTO = {
        nombre: formValues.nombre, codigo: formValues.codigo, categoriaId: formValues.categoriaId,
        estado: formValues.estado, estadoStock: formValues.estadoStock, conStock: null, precioMax: null
    };

    this.productoService.filtrarProductos(filtro, 0, 10000).subscribe({
      next: (page) => {
        this.isLoading = false;
        callback(page.content);
      },
      error: (err) => {
        this.isLoading = false;
        mostrarToast('Error al obtener datos para exportar.', 'danger');
      }
    });
  }

  exportarCatalogoPDF() {
    this.obtenerTodosParaExportar((productos) => {
        const doc = new jsPDF();
        
        const fechaHora = new Date().toLocaleString('es-AR'); 
        const usuario = localStorage.getItem('username') || localStorage.getItem('email') || 'Administrador';

        doc.setFontSize(16);
        doc.setFont('helvetica', 'bold');
        doc.text('Catálogo de Productos', 14, 20);
        
        doc.setFontSize(10);
        doc.setFont('helvetica', 'normal');
        doc.text(`Generado por: ${usuario}`, 14, 28);
        doc.text(`Fecha y Hora: ${fechaHora}`, 14, 34);
        doc.text(`Total de artículos: ${productos.length}`, 14, 40);

        autoTable(doc, {
            startY: 45, 
            head: [['Código', 'Nombre', 'Categoría', 'Stock', 'Precio ($)']],
            body: productos.map(p => [
                p.codigo || 'N/A',
                p.nombre,
                p.categoriaNombre || 'Sin Categoría',
                p.stockActual.toString(),
                `$ ${p.precioVenta.toLocaleString('es-AR')}`
            ]),
            theme: 'striped',
            headStyles: { fillColor: [33, 37, 41] }, 
        });

        const pageCount = (doc as any).internal.getNumberOfPages();
        const pageWidth = doc.internal.pageSize.getWidth();
        const pageHeight = doc.internal.pageSize.getHeight();

        for (let i = 1; i <= pageCount; i++) {
            doc.setPage(i);
            doc.setFontSize(9);
            doc.setTextColor(150); 
            
            doc.line(14, pageHeight - 15, pageWidth - 14, pageHeight - 15);
            doc.text(`Sistema POS Masterserv - Auditoría`, 14, pageHeight - 10);
            doc.text(`Página ${i} de ${pageCount}`, pageWidth - 14, pageHeight - 10, { align: 'right' });
        }

        const nombreArchivo = `Catalogo_${fechaHora.replace(/[\/:, ]/g, '-')}.pdf`;
        doc.save(nombreArchivo);
    });
  }

  exportarCatalogoExcel() {
    this.obtenerTodosParaExportar((productos) => {
        const fecha = new Date().toLocaleDateString().replace(/\//g, '-');
        
        const data = productos.map(p => ({
            'Código': p.codigo || 'N/A',
            'Nombre del Producto': p.nombre,
            'Categoría': p.categoriaNombre || 'Sin Categoría',
            'Stock Actual': p.stockActual,
            'Precio de Venta ($)': p.precioVenta,
            'Costo ($)': p.precioCosto || 0,
            'Estado': p.estado
        }));

        const ws: XLSX.WorkSheet = XLSX.utils.json_to_sheet(data);
        const wb: XLSX.WorkBook = XLSX.utils.book_new();
        XLSX.utils.book_append_sheet(wb, ws, 'Catálogo');

        XLSX.writeFile(wb, `Catalogo_Productos_${fecha}.xlsx`);
    });
  }
}