import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs'; // Importamos BehaviorSubject
import { tap } from 'rxjs/operators'; // Importamos tap para efectos secundarios
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

  // 1. FUENTE DE LA VERDAD (Estado del Logo en memoria)
  // Iniciamos con null o string vacío.
  private logoSubject = new BehaviorSubject<string>(''); 

  // 2. OBSERVABLE PÚBLICO (Para que el Sidebar se suscriba)
  public logo$ = this.logoSubject.asObservable();

  constructor(private http: HttpClient) { 
    // Al iniciar el servicio, cargamos la configuración inicial
    this.cargarConfiguracionInicial();
  }

  // Carga inicial automática
  private cargarConfiguracionInicial() {
    this.obtenerConfigPublica().subscribe({
      next: (config) => {
        if (config && config.logoUrl) {
          this.logoSubject.next(config.logoUrl);
        }
      },
      error: (err) => console.warn('No se pudo cargar config inicial', err)
    });
  }

  // --- MÉTODOS API ---

  getConfiguracion(): Observable<EmpresaConfig> {
    return this.http.get<EmpresaConfig>(this.apiUrl);
  }

  // 🔥 AQUÍ ESTÁ LA MAGIA:
  // Cuando actualizamos, usamos .pipe(tap(...)) para actualizar el logo localmente
  updateConfiguracion(config: EmpresaConfig): Observable<EmpresaConfig> {
    return this.http.put<EmpresaConfig>(this.apiUrl, config).pipe(
      tap((configActualizada) => {
        // Si la actualización fue exitosa, emitimos el nuevo logo a toda la app
        if (configActualizada.logoUrl) {
          this.logoSubject.next(configActualizada.logoUrl);
        }
      })
    );
  }

  // Corregí la ruta, sobraba un "/configuracion" extra
  obtenerConfigPublica(): Observable<EmpresaConfig> {
    return this.http.get<EmpresaConfig>(`${this.apiUrl}/publica`);
  }
}