import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';
import { Router } from '@angular/router';
import { JwtHelperService } from '@auth0/angular-jwt'; 

import { LoginRequestDTO } from '../models/login-request.model';
import { AuthResponseDTO } from '../models/auth-response.model';
import { RegisterRequestDTO } from '../models/register-request.model';

const API_URL = 'http://localhost:8080';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private http = inject(HttpClient);
  private router = inject(Router);
  private apiUrl = `${API_URL}/api/auth`; 
  private readonly TOKEN_KEY = 'jwt_token';
  private readonly jwtHelper = new JwtHelperService(); 

  // --- ¡CORRECCIÓN 1! ---
  // El estado inicial de 'loggedIn' ahora VERIFICA la expiración.
  private loggedIn = new BehaviorSubject<boolean>(this.hasToken());
  public isLoggedIn$ = this.loggedIn.asObservable();
  // --------------------

  private currentUserEmail = new BehaviorSubject<string | null>(this.getEmailFromToken());
  public currentUserEmail$ = this.currentUserEmail.asObservable();
  private currentUserRole = new BehaviorSubject<string | null>(this.getRoleFromToken());
  public currentUserRole$ = this.currentUserRole.asObservable();

  constructor() { }

  login(credentials: LoginRequestDTO): Observable<AuthResponseDTO> {
    return this.http.post<AuthResponseDTO>(`${this.apiUrl}/login`, credentials).pipe(
      tap(response => {
        this.saveToken(response.token);
        // Actualizamos todos los estados
        this.loggedIn.next(true);
        this.currentUserEmail.next(this.getEmailFromToken());
        this.currentUserRole.next(this.getRoleFromToken());
      })
    );
  }

  register(data: RegisterRequestDTO): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/register`, data);
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    // Reseteamos todos los estados
    this.loggedIn.next(false);
    this.currentUserEmail.next(null);
    this.currentUserRole.next(null);
    this.router.navigate(['/login']);
  }

  private saveToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  // --- ¡CORRECCIÓN 2! ---
  /**
   * Verifica si hay un token Y si NO ha expirado.
   */
  hasToken(): boolean {
    const token = this.getToken();
    // Revisa si el token existe Y si el jwtHelper dice que NO está expirado
    return !!token && !this.jwtHelper.isTokenExpired(token);
  }
  // --------------------

  public getDecodedToken(): any | null {
    const token = this.getToken();
    if (token) {
      try {
        return this.jwtHelper.decodeToken(token);
      } catch (e) {
        return null; // Token corrupto
      }
    }
    return null;
  }

  private getEmailFromToken(): string | null {
    const token = this.getDecodedToken();
    return token ? token.sub : null;
  }
  
  private getRoleFromToken(): string | null {
    const token = this.getDecodedToken();
    if (token && token.roles && token.roles.length > 0) {
      return token.roles[0];
    }
    return null;
  }

  public hasRole(roleName: string): boolean {
    const token = this.getDecodedToken();
    if (!token || !token.roles) {
      return false;
    }
    return token.roles.includes(roleName);
  }

  getApiUrlBase(): string {
    return API_URL;
  }
}