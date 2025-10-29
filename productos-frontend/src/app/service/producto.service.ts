// src/app/service/producto.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
// Asumimos que tienes la URL base definida como constante
// Si no, deberías tenerla en environment.ts
import { API_URL } from '../app.config'; // <-- ¡IMPORTANTE! Asegúrate de tener esta constante
import { ProductoDTO } from '../models/producto.model';
import { ProductoFiltroDTO } from '../models/producto-filtro.model';

// Interfaz para la respuesta paginada de Spring Boot
// (La tenías en tu archivo, la mantengo aquí)
export interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number; // Página actual (basada en 0)
}

@Injectable({
  providedIn: 'root'
})
export class ProductoService {

  private http = inject(HttpClient);
  // Asegúrate de que API_URL esté configurada, si no, reemplaza por 'http://localhost:8080'
  private apiUrl = `${API_URL}/api/productos`; 

  constructor() { }

  /**
   * Obtiene productos usando filtros y paginación.
   * Llama al endpoint POST /filtrar del backend.
   */
  filtrarProductos(filtro: ProductoFiltroDTO, page: number, size: number): Observable<Page<ProductoDTO>> {
    // Los parámetros de paginación van en la URL
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
    // Quitamos el ID y el nombre de categoría, el backend los ignora/genera
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
   * Este método (getProductosByProveedor) ya no lo usaremos para el formulario,
   * lo reemplazamos por 'searchProductosByProveedor'.
   * Lo dejamos aquí por si otra parte de tu app lo usa.
   */
  getProductosByProveedor(proveedorId: number): Observable<ProductoDTO[]> {
    // Hacemos la llamada GET a la URL que creamos en el ProductoController
    return this.http.get<ProductoDTO[]>(`${this.apiUrl}/por-proveedor/${proveedorId}`);
  }

  /**
   * --- ¡NUEVO MÉTODO DE BÚSQUEDA! ---
   * Llama al endpoint de búsqueda paginado del backend.
   */
  searchProductosByProveedor(
    proveedorId: number, 
    term: string, 
    page: number = 0, 
    size: number = 20
  ): Observable<Page<ProductoDTO>> {
    
    // Usamos HttpParams para construir la URL de forma segura
    let params = new HttpParams()
      .set('proveedorId', proveedorId.toString())
      .set('search', term)
      .set('page', page.toString())
      .set('size', size.toString());

    // Llamamos al nuevo endpoint que creamos en Spring Boot
    return this.http.get<Page<ProductoDTO>>(`${this.apiUrl}/search-by-proveedor`, { params });
  }
}