import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http'; 
import { Observable } from 'rxjs';
import { ClientePerfilDTO } from '../models/cliente-perfil.model';
import { ClientePerfilUpdateDTO } from '../models/cliente-perfil-update.model';
import { VentaResumenDTO } from '../models/venta-resumen.model'; 
import { Page } from '../models/page.model'; 
import { ClienteDTO } from '../models/cliente.dto'; // <--- IMPORTANTE: Importar el DTO nuevo

const API_URL = 'http://localhost:8080/api/cliente'; 

@Injectable({
  providedIn: 'root'
})
export class ClienteService {

  private http = inject(HttpClient);

  getMiPerfil(): Observable<ClientePerfilDTO> {
    return this.http.get<ClientePerfilDTO>(`${API_URL}/mi-perfil`);
  }

  updateMiPerfil(updateDTO: ClientePerfilUpdateDTO): Observable<ClientePerfilDTO> {
    return this.http.put<ClientePerfilDTO>(`${API_URL}/mi-perfil`, updateDTO);
  }

  getMisCompras(page: number = 0, size: number = 10): Observable<Page<VentaResumenDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
      
    return this.http.get<Page<VentaResumenDTO>>(`${API_URL}/mis-compras`, { params });
  }

  cambiarPassword(data: { passwordActual: string; passwordNueva: string }): Observable<void> {
    return this.http.patch<void>(`${API_URL}/cambiar-password`, data);
  }

  // --- MENTOR: ESTE ES EL MÉTODO QUE TE FALTABA ---
  registrarDesdePos(cliente: ClienteDTO): Observable<any> {
    // Apunta al endpoint nuevo del backend
    return this.http.post(`${API_URL}/registro-pos`, cliente);
  }
  // ------------------------------------------------
}