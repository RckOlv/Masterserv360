import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { Page } from '../models/page.model';
import { ProductoPublicoDTO } from '../models/producto-publico.model';
import { ProductoPublicoFiltroDTO } from '../models/producto-publico-filtro.model';
import { API_URL } from '../app.config';



@Injectable({
  providedIn: 'root'
})
export class CatalogoService {

  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/catalogo`;

  // --- Lógica de Búsqueda Global ---
  private searchTerm = new BehaviorSubject<string>('');
  public searchTerm$ = this.searchTerm.asObservable();

  public setSearchTerm(term: string): void { // <-- Devuelve 'void'
    this.searchTerm.next(term);
  }

  // --- ¡MÉTODO AÑADIDO! ---
  /**
   * Obtiene el valor actual del término de búsqueda.
   */
  public getSearchTermValue(): string {
    return this.searchTerm.getValue();
  }
  // -------------------------

  getCatalogoPaginado(page: number = 0, size: number = 12, sort: string = 'nombre,asc'): Observable<Page<ProductoPublicoDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);
      
    return this.http.get<Page<ProductoPublicoDTO>>(`${API_URL}/productos`, { params });
  }

  filtrarCatalogoPaginado(
    filtro: ProductoPublicoFiltroDTO, 
    page: number = 0, 
    size: number = 12, 
    sort: string = 'nombre,asc'
  ): Observable<Page<ProductoPublicoDTO>> {
    
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    return this.http.post<Page<ProductoPublicoDTO>>(`${API_URL}/productos/filtrar`, filtro, { params });
  }
}