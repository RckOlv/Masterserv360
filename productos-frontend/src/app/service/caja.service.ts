import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Caja {
  id: number;
  usuario: any;
  fechaApertura: string;
  fechaCierre?: string;
  montoInicial: number;
  ventasEfectivo: number;
  ventasTarjeta: number;
  ventasTransferencia: number;
  montoDeclarado?: number;
  diferencia?: number;
  estado: string;
}

@Injectable({
  providedIn: 'root'
})
export class CajaService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl + '/caja'; // Asegúrate de que apiUrl apunte a /api

  verificarCajaAbierta(usuarioId: number): Observable<Caja> {
    return this.http.get<Caja>(`${this.apiUrl}/abierta/${usuarioId}`);
  }

  abrirCaja(usuarioId: number, montoInicial: number): Observable<Caja> {
    return this.http.post<Caja>(`${this.apiUrl}/abrir`, { usuarioId, montoInicial });
  }

  cerrarCaja(cajaId: number, montoDeclarado: number): Observable<Caja> {
    return this.http.post<Caja>(`${this.apiUrl}/cerrar`, { cajaId, montoDeclarado });
  }
}