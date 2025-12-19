import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Auditoria } from '../models/auditoria.model';
import { Page } from '../models/page.model'; // Asegúrate de tener este modelo genérico
import { API_URL } from '../app.config';

@Injectable({
  providedIn: 'root'
})
export class AuditoriaService {
  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/auditoria`;

  getLogs(page: number = 0, size: number = 20): Observable<Page<Auditoria>> {
    return this.http.get<Page<Auditoria>>(`${this.apiUrl}?page=${page}&size=${size}`);
  }
}