import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../app.config';

// Definimos la interfaz aquí (o puedes ponerla en models/solicitud.model.ts)
export interface ListaEsperaItem {
  id: number;
  usuario: {
    nombre: string;
    apellido: string;
    telefono: string;
    email: string;
  };
  producto: {
    nombre: string;
    codigo: string;
    imagenUrl?: string; // Opcional por si quieres mostrar foto
  };
  fechaSolicitud: string;
  estado: 'PENDIENTE' | 'NOTIFICADA' | 'CANCELADA';
}

@Injectable({
  providedIn: 'root'
})
export class SolicitudService {
  private http = inject(HttpClient);
  
  // CAMBIO CLAVE: Apuntamos al nuevo controller
  private apiUrl = `${API_URL}/lista-espera`; 

  getAll(): Observable<ListaEsperaItem[]> {
    // Como tu controller tiene @GetMapping sin path extra, esto llama a /lista-espera
    return this.http.get<ListaEsperaItem[]>(this.apiUrl);
  }

  // Opcional: Eliminar registro de espera
  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}