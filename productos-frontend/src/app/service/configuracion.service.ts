import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface EmpresaConfig {
  id?: number;
  razonSocial: string;
  nombreFantasia: string;
  cuit: string;
  direccion: string;
  telefono: string;
  emailContacto: string;
  sitioWeb: string;
  logoUrl: string;
  colorPrincipal: string;
  piePaginaPresupuesto: string;
}

@Injectable({
  providedIn: 'root'
})
export class ConfiguracionService {

  private apiUrl = `${environment.apiUrl}/configuracion`;

  constructor(private http: HttpClient) { }

  getConfiguracion(): Observable<EmpresaConfig> {
    return this.http.get<EmpresaConfig>(this.apiUrl);
  }

  updateConfiguracion(config: EmpresaConfig): Observable<EmpresaConfig> {
    return this.http.put<EmpresaConfig>(this.apiUrl, config);
  }

  obtenerConfig(): Observable<any> {
  return this.http.get(`${this.apiUrl}/configuracion/publica`);
}
}