import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Page } from '../../models/page.model';
import { ProductoPublicoDTO } from '../../models/producto-publico.model';
import { CategoriaDTO } from '../../models/categoria.model';
import { CatalogoService } from '../../service/catalogo.service';
import { CategoriaService } from '../../service/categoria.service';
import { ProductoPublicoFiltroDTO } from '../../models/producto-publico-filtro.model';
import { mostrarToast } from '../../utils/toast';
import { PageEvent, MatPaginatorModule } from '@angular/material/paginator';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-catalogo',
  standalone: true,
  imports: [ CommonModule, ReactiveFormsModule, MatPaginatorModule ],
  templateUrl: './catalogo.html',
  styleUrl: './catalogo.css'
})
export default class CatalogoComponent implements OnInit, OnDestroy {

  private fb = inject(FormBuilder);
  private catalogoService = inject(CatalogoService);
  private categoriaService = inject(CategoriaService);
  
  public page?: Page<ProductoPublicoDTO>;
  public categorias: CategoriaDTO[] = [];
  public filtroForm: FormGroup;
  public isLoading = true;
  private currentSort = 'nombre,asc';

  private searchTermSub?: Subscription;
  public selectedCategoriaId: number | null = null;
  
  constructor() {
    this.filtroForm = this.fb.group({
      precioMin: [null, Validators.min(0)],
      precioMax: [null, Validators.min(0)],
      soloConStock: [false]
    });
  }

  ngOnInit(): void {
    this.loadCategorias();

    // Escuchamos la búsqueda de la Navbar.
    this.searchTermSub = this.catalogoService.searchTerm$.subscribe(() => {
      this.onFiltrar(0); // Filtra y resetea a la página 0
    });
  }

  ngOnDestroy(): void {
    this.searchTermSub?.unsubscribe();
    this.catalogoService.setSearchTerm('');
  }

  loadCategorias(): void {
    this.categoriaService.listarCategorias('ACTIVO').subscribe({
      next: (data) => this.categorias = data,
      error: (err) => mostrarToast('Error al cargar filtros de categoría', 'warning')
    });
  }

  /**
   * Método ÚNICO que lee todos los filtros y llama a la API.
   */
  onFiltrar(pageIndex: number = 0): void {
    this.isLoading = true;
    const formValues = this.filtroForm.value;
    const searchTerm = this.catalogoService.getSearchTermValue(); 
    const size = this.page?.size || 12;
  
    const filtro: ProductoPublicoFiltroDTO = {
      nombre: searchTerm || undefined, 
      categoriaIds: this.selectedCategoriaId ? [this.selectedCategoriaId] : undefined,
      precioMin: formValues.precioMin || undefined,
      precioMax: formValues.precioMax || undefined,
      soloConStock: formValues.soloConStock ? true : undefined
    };
  
    this.catalogoService.filtrarCatalogoPaginado(filtro, pageIndex, size, this.currentSort).subscribe({
      next: (data) => { this.page = data; this.isLoading = false; },
      error: (err) => { mostrarToast('Error al aplicar los filtros', 'danger'); this.isLoading = false; }
    });
  }

  /**
   * ¡CORREGIDO!
   * Acepta 'number | undefined' (de cat.id) Y 'null' (de "Ver Todas").
   */
  filtrarPorCategoria(id: number | null | undefined): void {
    // Si el ID es undefined (de un cat.id opcional) O null (de "Ver Todas")
    const newId = (id === undefined || id === null) ? null : id; 
    
    // Si vuelve a clickear la misma, la deselecciona (null)
    this.selectedCategoriaId = (this.selectedCategoriaId === newId) ? null : newId;
    
    // Limpiamos el término de búsqueda de la navbar
    this.catalogoService.setSearchTerm(''); 
    // onFiltrar(0) se disparará automáticamente por la suscripción a searchTerm$
  }

  /**
   * Limpia los filtros y el término de búsqueda.
   */
  limpiarFiltros(): void {
    this.filtroForm.reset({ soloConStock: false });
    this.selectedCategoriaId = null;
    this.catalogoService.setSearchTerm(''); 
  }

  /**
   * Maneja el evento de cambio de página.
   */
  handlePageEvent(e: PageEvent): void {
    this.onFiltrar(e.pageIndex);
  }
}