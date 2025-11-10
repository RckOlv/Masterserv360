import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../app.config';
import { CotizacionPublicaDTO } from '../models/cotizacion-publica.model';
import { OfertaProveedorDTO } from '../models/oferta-proveedor.model';

/**
 * Este servicio maneja todas las llamadas a los endpoints /api/public
 * que no requieren un token de autenticación JWT.
 */
@Injectable({
  providedIn: 'root'
})
export class PublicService {

  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/api/public`; // URL base pública

  /**
   * Obtiene los detalles de una cotización usando el token público.
   * Llama a: GET /api/public/oferta/{token}
   */
  getCotizacionPorToken(token: string): Observable<CotizacionPublicaDTO> {
    return this.http.get<CotizacionPublicaDTO>(`${this.apiUrl}/oferta/${token}`);
  }

  /**
   * Envía la oferta (precios y fecha) llenada por el proveedor.
   * Llama a: POST /api/public/oferta/{token}
   */
  submitOferta(token: string, oferta: OfertaProveedorDTO): Observable<any> {
    // El backend devuelve un Map<String, String> ({ "status": "ok", ... }),
    // por eso usamos 'any'.
    return this.http.post<any>(`${this.apiUrl}/oferta/${token}`, oferta);
  }
}