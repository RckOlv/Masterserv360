import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
// import { environment } from '../../environments/environment'; // <-- LÍNEA ELIMINADA
import { Observable, BehaviorSubject, tap } from 'rxjs';
import { LoginRequestDTO } from '../models/login-request.model';
import { AuthResponseDTO } from '../models/auth-response.model';
import { RegisterRequestDTO } from '../models/register-request.model';
import { Router } from '@angular/router';

// Solución Rápida: Definimos la URL de la API aquí
const API_URL = 'http://localhost:8080'; // Esta es la URL de tu backend Spring Boot

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private http = inject(HttpClient);
  private router = inject(Router);
  private apiUrl = `${API_URL}/api/auth`; // Usamos la constante
  private readonly TOKEN_KEY = 'jwt_token';

  private loggedIn = new BehaviorSubject<boolean>(this.hasToken());
  isLoggedIn$ = this.loggedIn.asObservable();

  constructor() { }

  login(credentials: LoginRequestDTO): Observable<AuthResponseDTO> {
    return this.http.post<AuthResponseDTO>(`${this.apiUrl}/login`, credentials).pipe(
      tap(response => {
        this.saveToken(response.token);
        this.loggedIn.next(true);
      })
    );
  }

  register(data: RegisterRequestDTO): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/register`, data);
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    this.loggedIn.next(false);
    this.router.navigate(['/login']);
  }

  private saveToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  hasToken(): boolean {
    return !!this.getToken();
  }

  // --- Helper para el Interceptor ---
  // El interceptor necesita saber la URL base para no enviar el
  // token a APIs de terceros (ej. Google Maps)
  getApiUrlBase(): string {
    return API_URL;
  }
}