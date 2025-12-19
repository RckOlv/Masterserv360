import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../app.config';
import { TipoDocumentoDTO } from '../models/tipo-documento.model';

@Injectable({
  providedIn: 'root'
})
export class TipoDocumentoService {
  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/tipos-documento`; // Endpoint del backend

  constructor() { }

  /**
   * Obtiene la lista de Tipos de Documento (Endpoint público).
   */
  listarTiposDocumento(): Observable<TipoDocumentoDTO[]> {
    return this.http.get<TipoDocumentoDTO[]>(this.apiUrl);
  }
}