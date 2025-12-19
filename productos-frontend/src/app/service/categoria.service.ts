import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http'; // <-- Importar HttpParams
import { Observable } from 'rxjs';
import { API_URL } from '../app.config';
import { CategoriaDTO } from '../models/categoria.model';

@Injectable({
  providedIn: 'root'
})
export class CategoriaService {

  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/categorias`; // Endpoint del backend

  constructor() { }

  /**
   * Obtiene las categorías. AHORA ACEPTA UN FILTRO DE ESTADO.
   */
  listarCategorias(estado?: string | null): Observable<CategoriaDTO[]> {
    let params = new HttpParams();
    if (estado && estado !== 'TODOS') { // Si el estado es 'TODOS', no enviamos parámetro
      params = params.set('estado', estado); // Envía ?estado=ACTIVO o ?estado=INACTIVO
    }
    
    // Llama a: GET /api/categorias?estado=...
    return this.http.get<CategoriaDTO[]>(this.apiUrl, { params });
  }

  getById(id: number): Observable<CategoriaDTO> {
    return this.http.get<CategoriaDTO>(`${this.apiUrl}/${id}`);
  }

  crear(categoria: CategoriaDTO): Observable<CategoriaDTO> {
    const { id, ...categoriaParaCrear } = categoria;
    return this.http.post<CategoriaDTO>(this.apiUrl, categoriaParaCrear);
  }

  actualizar(categoria: CategoriaDTO): Observable<CategoriaDTO> {
     if (!categoria.id) {
       throw new Error('ID de categoría es requerido para actualizar');
     }
     return this.http.put<CategoriaDTO>(`${this.apiUrl}/${categoria.id}`, categoria);
  }

  softDelete(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/${id}`);
  }

  reactivar(id: number): Observable<any> {
     return this.http.patch<any>(`${this.apiUrl}/${id}/reactivar`, {});
  }
}