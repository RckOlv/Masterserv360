import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../app.config';

// ✅ INTERFAZ PLANA (Coincide con el ListaEsperaDTO de Java)
export interface ListaEsperaItem {
  id: number;
  fechaSolicitud: string;
  estado: 'PENDIENTE' | 'NOTIFICADA' | 'CANCELADA';
  
  // Datos del Usuario (Vienen directos del DTO)
  usuarioNombre: string;
  usuarioApellido: string;
  usuarioTelefono: string;
  usuarioEmail: string;

  // Datos del Producto (Vienen directos del DTO)
  productoNombre: string;
  productoCodigo: string;
}

@Injectable({
  providedIn: 'root'
})
export class SolicitudService {
  private http = inject(HttpClient);
  
  // Apunta al nuevo endpoint de la lista de espera
  private apiUrl = `${API_URL}/lista-espera`; 

  getAll(): Observable<ListaEsperaItem[]> {
    return this.http.get<ListaEsperaItem[]>(this.apiUrl);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}