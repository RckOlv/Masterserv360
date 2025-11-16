import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SaldoPuntosDTO } from '../models/saldo-puntos.model';
import { CanjePuntosRequestDTO } from '../models/canje-puntos-request.model';
import { CuponDTO } from '../models/cupon.model';

// Asumo que tu URL base está centralizada, si no, ponla aquí
const API_URL = 'http://localhost:8080/api/puntos'; 

@Injectable({
  providedIn: 'root'
})
export class PuntosService {

  private http = inject(HttpClient);

  /**
   * Llama al backend (GET /api/puntos/mi-saldo)
   * para obtener el saldo de puntos del cliente autenticado.
   */
  getMiSaldo(): Observable<SaldoPuntosDTO> {
    return this.http.get<SaldoPuntosDTO>(`${API_URL}/saldo`);
  }

  /**
   * Llama al backend (POST /api/puntos/canjear)
   * para canjear puntos por un cupón.
   */
  canjearPuntos(puntos: number): Observable<CuponDTO> {
    const request: CanjePuntosRequestDTO = {
      puntosACanjear: puntos
    };
    return this.http.post<CuponDTO>(`${API_URL}/canjear`, request);
  }
}