import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http'; // Asegúrate de importar HttpParams
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment'; 
import { ProductoDTO } from '../models/producto.model';
import { Page } from '../models/page.model';
import { ProductoFiltroDTO } from '../models/producto-filtro.model';

@Injectable({
  providedIn: 'root'
})
export class ProductoService {

  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/productos`;

  constructor() { }

  getProductos(page: number, size: number): Observable<Page<ProductoDTO>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
      
    return this.http.get<Page<ProductoDTO>>(this.apiUrl, { params });
  }

  getProductoById(id: number): Observable<ProductoDTO> {
    return this.http.get<ProductoDTO>(`${this.apiUrl}/${id}`);
  }

  // --- MENTOR: NUEVO MÉTODO PARA GENERAR CÓDIGO ---
  generarCodigo(categoriaId: number, nombre: string): Observable<{ codigo: string }> {
    return this.http.get<{ codigo: string }>(`${this.apiUrl}/generar-codigo`, {
      params: { categoriaId: categoriaId.toString(), nombre }
    });
  }
  // -----------------------------------------------

  searchProductosByProveedor(proveedorId: number, term: string): Observable<Page<ProductoDTO>> {
    let params = new HttpParams()
      .set('proveedorId', proveedorId.toString())
      .set('search', term)
      .set('page', '0')
      .set('size', '20'); // Límite para el buscador

    return this.http.get<Page<ProductoDTO>>(`${this.apiUrl}/search-by-proveedor`, { params });
  }

  filtrarProductos(filtro: ProductoFiltroDTO, page: number, size: number): Observable<Page<ProductoDTO>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.post<Page<ProductoDTO>>(`${this.apiUrl}/filtrar`, filtro, { params });
  }

  crearProducto(producto: ProductoDTO): Observable<ProductoDTO> {
    return this.http.post<ProductoDTO>(this.apiUrl, producto);
  }

  actualizarProducto(id: number, producto: ProductoDTO): Observable<ProductoDTO> {
    return this.http.put<ProductoDTO>(`${this.apiUrl}/${id}`, producto);
  }

  eliminarProducto(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }

  getProductosPorProveedor(proveedorId: number): Observable<ProductoDTO[]> {
    return this.http.get<ProductoDTO[]>(`${this.apiUrl}/por-proveedor/${proveedorId}`);
  }
}