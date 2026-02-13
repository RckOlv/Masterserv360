import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../app.config'; // O tu environment

export interface Alerta {
  id: number;
  titulo: string;
  mensaje: string;
  fecha: string; // LocalDateTime
  leida: boolean;
  urlDestino?: string; // Para redirigir al cliente específico
}

@Injectable({
  providedIn: 'root'
})
export class AlertaService {
  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/alertas`;

  getNoLeidas(): Observable<Alerta[]> {
    return this.http.get<Alerta[]>(`${this.apiUrl}/no-leidas`);
  }

  marcarLeida(id: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${id}/leer`, {});
  }
  
  marcarTodasLeidas(): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/leer-todas`, {});
  }
}