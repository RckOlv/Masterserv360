import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { SolicitudProducto } from '../models/solicitud.model';

@Injectable({
  providedIn: 'root'
})
export class SolicitudService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/solicitudes`;

  getAll(): Observable<SolicitudProducto[]> {
    return this.http.get<SolicitudProducto[]>(this.apiUrl);
  }

  marcarProcesada(id: number): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/procesar`, {});
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}