import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SaldoPuntos } from '../models/saldo-puntos.model';
import { API_URL } from '../app.config';
import { ClienteFidelidadDTO } from '../models/cliente-fidelidad.dto';

@Injectable({
  providedIn: 'root'
})
export class PuntosService {

  // Asegúrate que esta URL coincida con tu backend
   private apiUrl = `${API_URL}/puntos`;

  constructor(private http: HttpClient) { }

  getMiSaldo(): Observable<SaldoPuntos> {
    return this.http.get<SaldoPuntos>(`${this.apiUrl}/mi-saldo`);
  }

  /**
   * Canjea puntos por una recompensa específica.
   * Se envía el ID como parámetro: POST /puntos/canje?recompensaId=1
   */
  canjearPuntos(recompensaId: number): Observable<any> {
    const params = new HttpParams().set('recompensaId', recompensaId.toString());
    return this.http.post<any>(`${this.apiUrl}/canje`, null, { params });
  }

  getFidelidadCliente(clienteId: number): Observable<ClienteFidelidadDTO> {
    return this.http.get<ClienteFidelidadDTO>(`${this.apiUrl}/cliente/${clienteId}/fidelidad`);
  }
}