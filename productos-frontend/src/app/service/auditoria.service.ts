import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Auditoria } from '../models/auditoria.model';
import { Page } from '../models/page.model';
import { API_URL } from '../app.config';

@Injectable({
  providedIn: 'root'
})
export class AuditoriaService {
  private http = inject(HttpClient);
  private apiUrl = `${API_URL}/auditoria`;

  // Mantenemos este por compatibilidad, pero usaremos principalmente el filtrar
  getLogs(page: number = 0, size: number = 20): Observable<Page<Auditoria>> {
    return this.http.get<Page<Auditoria>>(`${this.apiUrl}?page=${page}&size=${size}`);
  }

  // --- NUEVO MÉTODO PARA FILTRAR ---
  filtrarLogs(filtro: any, page: number, size: number): Observable<Page<Auditoria>> {
    const params = new HttpParams()
        .set('page', page.toString())
        .set('size', size.toString())
        .set('sort', 'fecha,desc'); // Siempre ordenamos por lo más reciente

    return this.http.post<Page<Auditoria>>(`${this.apiUrl}/filtrar`, filtro, { params });
  }
}