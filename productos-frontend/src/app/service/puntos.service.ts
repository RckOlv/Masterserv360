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

  // URL base
  private apiUrl = `${API_URL}/puntos`;

  constructor(private http: HttpClient) { }

  /**
   * Obtiene saldo del cliente logueado (Portal Cliente).
   */
  getMiSaldo(): Observable<SaldoPuntos> {
    return this.http.get<SaldoPuntos>(`${this.apiUrl}/mi-saldo`);
  }

  /**
   * Canjea puntos por una recompensa (Portal Cliente).
   * Usa el token del usuario logueado.
   */
  canjearPuntos(recompensaId: number): Observable<any> {
    const params = new HttpParams().set('recompensaId', recompensaId.toString());
    return this.http.post<any>(`${this.apiUrl}/canje`, null, { params });
  }

  /**
   * Canje ESPECIAL para POS (Vendedor).
   * Permite que el vendedor canjee en nombre de un cliente específico.
   */
  canjearPuntosPos(clienteId: number, recompensaId: number): Observable<any> {
    const params = new HttpParams()
        .set('clienteId', clienteId.toString())
        .set('recompensaId', recompensaId.toString());
        
    return this.http.post<any>(`${this.apiUrl}/canje-pos`, null, { params });
  }

  /**
   * Obtiene la info completa de fidelidad para el POS.
   */
  getFidelidadCliente(clienteId: number): Observable<ClienteFidelidadDTO> {
    // IMPORTANTE: No repetimos '/puntos' aquí
    return this.http.get<ClienteFidelidadDTO>(`${this.apiUrl}/cliente/${clienteId}/fidelidad`);
  }
}