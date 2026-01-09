import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http'; 
import { Observable } from 'rxjs';
import { ClientePerfilDTO } from '../models/cliente-perfil.model';
import { ClientePerfilUpdateDTO } from '../models/cliente-perfil-update.model';
import { VentaResumenDTO } from '../models/venta-resumen.model'; 
import { Page } from '../models/page.model'; 
import { ClienteDTO } from '../models/cliente.dto'; 
import { API_URL } from '../app.config';
import { CuponDTO } from '../models/cupon.model';

@Injectable({
  providedIn: 'root'
})
export class ClienteService {

  private http = inject(HttpClient);
  // Definimos la base correcta para este servicio
  private baseUrl = `${API_URL}/cliente`;

  getMiPerfil(): Observable<ClientePerfilDTO> {
    // CORREGIDO: Usamos baseUrl
    return this.http.get<ClientePerfilDTO>(`${this.baseUrl}/mi-perfil`);
  }

  updateMiPerfil(updateDTO: ClientePerfilUpdateDTO): Observable<ClientePerfilDTO> {
    // CORREGIDO: Usamos baseUrl
    return this.http.put<ClientePerfilDTO>(`${this.baseUrl}/mi-perfil`, updateDTO);
  }

  getMisCompras(page: number = 0, size: number = 10): Observable<Page<VentaResumenDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
      
    // CORREGIDO: Ahora apunta a /api/cliente/mis-compras
    return this.http.get<Page<VentaResumenDTO>>(`${this.baseUrl}/mis-compras`, { params });
  }

  cambiarPassword(data: { passwordActual: string; passwordNueva: string }): Observable<void> {
    // CORREGIDO: Usamos baseUrl
    return this.http.patch<void>(`${this.baseUrl}/cambiar-password`, data);
  }

  registrarDesdePos(cliente: ClienteDTO): Observable<any> {
    // CORREGIDO: Usamos baseUrl
    return this.http.post(`${this.baseUrl}/registro-pos`, cliente);
  }

  getMisCupones(): Observable<CuponDTO[]> {
    return this.http.get<CuponDTO[]>(`${this.baseUrl}/mis-cupones`);
  }
}