import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Permiso } from '../models/permiso.model';

@Injectable({ providedIn: 'root' })
export class PermisoService {
  private apiUrl = 'http://localhost:8080/permisos'; // ajustá tu URL

  constructor(private http: HttpClient) {}

  listarPermisos(): Observable<Permiso[]> {
    return this.http.get<Permiso[]>(this.apiUrl);
  }

  crearPermiso(permiso: Permiso): Observable<Permiso> {
    return this.http.post<Permiso>(this.apiUrl, permiso);
  }

  actualizarPermiso(permiso: Permiso): Observable<Permiso> {
    return this.http.put<Permiso>(`${this.apiUrl}/${permiso.id}`, permiso);
  }

  eliminarPermiso(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
