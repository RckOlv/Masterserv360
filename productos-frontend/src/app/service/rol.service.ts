import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from '../app.config';
import { RolDTO } from '../models/rol.model'; 

@Injectable({
  providedIn: 'root'
})
export class RolService {
  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/api/roles`; // Asumo esta ruta

  listarRoles(): Observable<RolDTO[]> { // <-- Corregido
    return this.http.get<RolDTO[]>(this.apiUrl); // <-- Corregido
  }
  
}