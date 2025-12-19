// src/app/service/regla-puntos.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../app.config';
import { ReglaPuntosDTO } from '../models/regla-puntos.model'; // <-- Necesitas crear este modelo

@Injectable({
  providedIn: 'root'
})
export class ReglaPuntosService {

  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/reglas-puntos`; // <-- Endpoint del backend

  constructor() { }

  /**
   * Obtiene el historial de todas las reglas de puntos (activas y caducadas).
   * Llama a: GET /reglas-puntos
   */
  listarReglas(): Observable<ReglaPuntosDTO[]> {
    return this.http.get<ReglaPuntosDTO[]>(this.apiUrl);
  }

  /**
   * Crea una nueva regla de puntos.
   * Esta acción desactivará la regla activa anterior en el backend.
   * Llama a: POST /reglas-puntos
   * @param regla DTO con la nueva configuración (montoGasto, puntosGanados, etc.)
   */
  crearNuevaReglaActiva(regla: ReglaPuntosDTO): Observable<ReglaPuntosDTO> {
    // El backend espera el DTO sin ID, estado, etc.
    const { id, estadoRegla, fechaInicioVigencia, ...reglaParaCrear } = regla;
    return this.http.post<ReglaPuntosDTO>(this.apiUrl, reglaParaCrear);
  }

  /**
   * Obtiene la regla activa actualmente.
   * Llama a: GET /reglas-puntos/activa
   */
  getReglaActiva(): Observable<ReglaPuntosDTO> {
     return this.http.get<ReglaPuntosDTO>(`${this.apiUrl}/activa`);
  }

}