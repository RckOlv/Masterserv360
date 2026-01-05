import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../app.config';
import { RecompensaDTO } from '../models/recompensa.model';

@Injectable({
  providedIn: 'root'
})
export class RecompensaService {

  private http = inject(HttpClient);
  // Asegúrate que API_URL termine en /api o ajusta aquí según tu backend
  private apiUrl = `${API_URL}/recompensas`; 

  // Para el Admin (Panel de Gestión)
  listarTodas(): Observable<RecompensaDTO[]> {
    return this.http.get<RecompensaDTO[]>(this.apiUrl);
  }

  // Para el Cliente (Catálogo de Canje)
  listarDisponibles(): Observable<RecompensaDTO[]> {
    return this.http.get<RecompensaDTO[]>(`${this.apiUrl}/disponibles`);
  }

  crear(recompensa: RecompensaDTO): Observable<RecompensaDTO> {
    return this.http.post<RecompensaDTO>(this.apiUrl, recompensa);
  }

  actualizar(id: number, recompensa: RecompensaDTO): Observable<RecompensaDTO> {
    return this.http.put<RecompensaDTO>(`${this.apiUrl}/${id}`, recompensa);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}