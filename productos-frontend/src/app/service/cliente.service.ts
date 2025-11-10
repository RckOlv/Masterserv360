import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http'; // ¡Importar HttpParams!
import { Observable } from 'rxjs';
import { ClientePerfilDTO } from '../models/cliente-perfil.model';
import { ClientePerfilUpdateDTO } from '../models/cliente-perfil-update.model';
import { VentaResumenDTO } from '../models/venta-resumen.model'; // <-- ¡DTO AÑADIDO!
import { Page } from '../models/page.model'; // <-- ¡PAGE AÑADIDO!

// Asumo que tu URL base está centralizada, si no, ponla aquí
const API_URL = 'http://localhost:8080/api/cliente'; 

@Injectable({
  providedIn: 'root'
})
export class ClienteService {

  private http = inject(HttpClient);

  /**
   * Llama al backend (GET /api/cliente/mi-perfil)
   * para obtener los datos del cliente autenticado.
   */
  getMiPerfil(): Observable<ClientePerfilDTO> {
    return this.http.get<ClientePerfilDTO>(`${API_URL}/mi-perfil`);
  }

  /**
   * Llama al backend (PUT /api/cliente/mi-perfil)
   * para actualizar los datos del cliente autenticado.
   */
  updateMiPerfil(updateDTO: ClientePerfilUpdateDTO): Observable<ClientePerfilDTO> {
    return this.http.put<ClientePerfilDTO>(`${API_URL}/mi-perfil`, updateDTO);
  }

  // --- ¡NUEVO MÉTODO AÑADIDO! ---

  /**
   * Llama al backend (GET /api/cliente/mis-compras)
   * para obtener el historial de compras paginado del cliente.
   */
  getMisCompras(page: number = 0, size: number = 10): Observable<Page<VentaResumenDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
      // (El backend se encarga de ordenar por fecha desc)
      
    return this.http.get<Page<VentaResumenDTO>>(`${API_URL}/mis-compras`, { params });
  }
}