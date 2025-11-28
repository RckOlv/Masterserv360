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
  private apiUrl = `${API_URL}/api/recompensas`; // (Necesitaremos crear este Controller en el Backend)

  // (Por ahora solo definimos los métodos, el backend lo hacemos después)

  crear(recompensa: RecompensaDTO): Observable<RecompensaDTO> {
    return this.http.post<RecompensaDTO>(this.apiUrl, recompensa);
  }

  actualizar(id: number, recompensa: RecompensaDTO): Observable<RecompensaDTO> {
    return this.http.put<RecompensaDTO>(`${this.apiUrl}/${id}`, recompensa);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
  
  // (No necesitamos un 'listar' porque las recompensas vendrán
  // anidadas dentro de la ReglaPuntosDTO)
}