import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../app.config'; 
import { ProductoDTO } from '../models/producto.model';
import { ProductoFiltroDTO } from '../models/producto-filtro.model';

// --- CORRECCIÓN: Importamos la interfaz Page COMPLETA desde el modelo ---
// (Ya no la definimos aquí abajo para evitar conflictos)
import { Page } from '../models/page.model'; 

@Injectable({
  providedIn: 'root'
})
export class ProductoService {

  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/api/productos`; 

  constructor() { }

  /**
   * Obtiene productos usando filtros y paginación.
   * Llama al endpoint POST /filtrar del backend.
   */
  filtrarProductos(filtro: ProductoFiltroDTO, page: number, size: number): Observable<Page<ProductoDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.post<Page<ProductoDTO>>(`${this.apiUrl}/filtrar`, filtro, { params });
  }

  /**
   * Obtiene un producto por su ID.
   */
  getProductoById(id: number): Observable<ProductoDTO> {
    return this.http.get<ProductoDTO>(`${this.apiUrl}/${id}`);
  }

  /**
   * Crea un nuevo producto.
   */
  crearProducto(producto: ProductoDTO): Observable<ProductoDTO> {
    // Desestructuramos para quitar ID y categoriaNombre si vienen
    const { id, categoriaNombre, ...productoParaCrear } = producto;
    return this.http.post<ProductoDTO>(this.apiUrl, productoParaCrear);
  }

  /**
   * Actualiza un producto existente.
   */
  actualizarProducto(id: number, producto: ProductoDTO): Observable<ProductoDTO> {
    const { categoriaNombre, ...productoParaActualizar } = producto;
    return this.http.put<ProductoDTO>(`${this.apiUrl}/${id}`, productoParaActualizar);
  }

  /**
   * Elimina un producto por su ID.
   */
  eliminarProducto(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/${id}`);
  }

  /**
   * Obtiene productos por proveedor (Método simple)
   */
  getProductosByProveedor(proveedorId: number): Observable<ProductoDTO[]> {
    return this.http.get<ProductoDTO[]>(`${this.apiUrl}/por-proveedor/${proveedorId}`);
  }

  /**
   * Búsqueda paginada de productos por proveedor.
   */
  searchProductosByProveedor(
    proveedorId: number, 
    term: string, 
    page: number = 0, 
    size: number = 20
  ): Observable<Page<ProductoDTO>> {
    
    let params = new HttpParams()
      .set('proveedorId', proveedorId.toString())
      .set('search', term)
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<Page<ProductoDTO>>(`${this.apiUrl}/search-by-proveedor`, { params });
  }
}