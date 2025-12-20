import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { Page } from '../../models/page.model';
import { ProductoPublicoDTO } from '../../models/producto-publico.model';
import { CategoriaDTO } from '../../models/categoria.model';
import { CatalogoService } from '../../service/catalogo.service';
import { CategoriaService } from '../../service/categoria.service';
import { ProductoPublicoFiltroDTO } from '../../models/producto-publico-filtro.model';
import { mostrarToast } from '../../utils/toast';
import { Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

@Component({
  selector: 'app-catalogo',
  standalone: true,
  imports: [ CommonModule, ReactiveFormsModule ],
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
  
  public searchControl = new FormControl(''); 
  
  public isLoading = true;
  private currentSort = 'nombre,asc';
  private searchSub?: Subscription;
  public showMobileFilters = false;

  // CAMBIO: Usamos un Set para manejar múltiples IDs seleccionados
  public selectedCategoriaIds: Set<number> = new Set();
  
  constructor() {
    this.filtroForm = this.fb.group({
      precioMin: [null, Validators.min(0)],
      precioMax: [null, Validators.min(0)],
      soloConStock: [false]
    });
  }

  ngOnInit(): void {
    this.loadCategorias();

    this.searchSub = this.searchControl.valueChanges.pipe(
      debounceTime(500), 
      distinctUntilChanged()
    ).subscribe(() => {
      this.onFiltrar(0); 
    });

    this.onFiltrar(0);
  }

  ngOnDestroy(): void {
    this.searchSub?.unsubscribe();
  }

  loadCategorias(): void {
    this.categoriaService.listarCategorias('ACTIVO').subscribe({
      next: (data) => this.categorias = data,
      error: (err) => mostrarToast('Error al cargar filtros de categoría', 'warning')
    });
  }

  onFiltrar(pageIndex: number = 0): void {
    this.isLoading = true;
    const formValues = this.filtroForm.value;
    const size = 12; 
  
    // Convertimos el Set a Array para enviarlo al backend
    const idsArray = Array.from(this.selectedCategoriaIds);

    const filtro: ProductoPublicoFiltroDTO = {
      nombre: this.searchControl.value || undefined, 
      // Si el array está vacío, enviamos undefined para traer todas
      categoriaIds: idsArray.length > 0 ? idsArray : undefined,
      precioMin: formValues.precioMin || undefined,
      precioMax: formValues.precioMax || undefined,
      soloConStock: formValues.soloConStock ? true : undefined
    };
  
    this.catalogoService.filtrarCatalogoPaginado(filtro, pageIndex, size, this.currentSort).subscribe({
      next: (data) => { this.page = data; this.isLoading = false; },
      error: (err) => { mostrarToast('Error al aplicar los filtros', 'danger'); this.isLoading = false; }
    });
  }

  // CAMBIO: Lógica de selección múltiple (Toggle)
  filtrarPorCategoria(id: number | null | undefined): void {
    if (id === null || id === undefined) {
      // Si eligen "Todas", limpiamos la selección
      this.selectedCategoriaIds.clear();
    } else {
      if (this.selectedCategoriaIds.has(id)) {
        // Si ya estaba, lo quitamos
        this.selectedCategoriaIds.delete(id);
      } else {
        // Si no estaba, lo agregamos
        this.selectedCategoriaIds.add(id);
      }
    }
    
    // NOTA: Ya no reseteamos el buscador al cambiar categoría, para permitir filtros combinados (ej: "Aceite" en categoría "Motor")
    // this.searchControl.setValue('', {emitEvent: false}); 
    
    this.onFiltrar(0);
  }

  limpiarFiltros(): void {
    this.filtroForm.reset({ soloConStock: false });
    this.selectedCategoriaIds.clear(); // Limpiamos el Set
    this.searchControl.setValue(''); 
    this.onFiltrar(0);
  }

  // --- MÉTODOS DE PAGINACIÓN MANUAL ---
  paginaAnterior(): void {
    if (this.page && !this.page.first) {
      this.onFiltrar(this.page.number - 1);
    }
  }

  paginaSiguiente(): void {
    if (this.page && !this.page.last) {
      this.onFiltrar(this.page.number + 1);
    }
  }

  toggleMobileFilters() {
    this.showMobileFilters = !this.showMobileFilters;
  }
  
  // Helper para el HTML: verifica si una categoría está seleccionada
  isCategoriaSelected(id: number | undefined): boolean {
      if (id === undefined || id === null) {
          return false;
      }
      return this.selectedCategoriaIds.has(id);
  }
}