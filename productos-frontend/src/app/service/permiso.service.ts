import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PermisoDTO } from '../models/permiso.model';

@Injectable({ providedIn: 'root' })
export class PermisoService {
  private apiUrl = 'http://localhost:8080/permisos'; // ajustá tu URL

  constructor(private http: HttpClient) {}

  listarPermisos(): Observable<PermisoDTO[]> {
    return this.http.get<PermisoDTO[]>(this.apiUrl);
  }

  crearPermiso(permiso: PermisoDTO): Observable<PermisoDTO> {
    return this.http.post<PermisoDTO>(this.apiUrl, permiso);
  }

  actualizarPermiso(permiso: PermisoDTO): Observable<PermisoDTO> {
    return this.http.put<PermisoDTO>(`${this.apiUrl}/${permiso.id}`, permiso);
  }

  eliminarPermiso(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
