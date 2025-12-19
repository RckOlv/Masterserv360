import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SaldoPuntos } from '../models/saldo-puntos.model';

@Injectable({
  providedIn: 'root'
})
export class PuntosService {

  // Asegúrate que esta URL coincida con tu backend
  private apiUrl = 'https://masterserv-backend.onrender.com/api/puntos';

  constructor(private http: HttpClient) { }

  getMiSaldo(): Observable<SaldoPuntos> {
    return this.http.get<SaldoPuntos>(`${this.apiUrl}/mi-saldo`);
  }

  /**
   * Canjea puntos por una recompensa específica.
   * Se envía el ID como parámetro: POST /api/puntos/canje?recompensaId=1
   */
  canjearPuntos(recompensaId: number): Observable<any> {
    const params = new HttpParams().set('recompensaId', recompensaId.toString());
    return this.http.post<any>(`${this.apiUrl}/canje`, null, { params });
  }
}