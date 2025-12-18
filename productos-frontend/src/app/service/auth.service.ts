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
  private readonly PERMISSIONS_KEY = 'user_permissions';
  
  private readonly jwtHelper = new JwtHelperService(); 

  private loggedIn = new BehaviorSubject<boolean>(this.hasToken());
  public isLoggedIn$ = this.loggedIn.asObservable();
  
  private currentUserEmail = new BehaviorSubject<string | null>(this.getEmailFromToken());
  public currentUserEmail$ = this.currentUserEmail.asObservable();
  
  private currentUserRole = new BehaviorSubject<string | null>(this.getRoleFromToken());
  public currentUserRole$ = this.currentUserRole.asObservable();

  private currentUserPermissions = new BehaviorSubject<string[]>(this.getPermissionsFromStorage());
  public currentUserPermissions$ = this.currentUserPermissions.asObservable();

  constructor() { }

  login(credentials: LoginRequestDTO): Observable<AuthResponseDTO> {
    return this.http.post<AuthResponseDTO>(`${this.apiUrl}/login`, credentials).pipe(
      tap(response => {
        // Solo guardamos el token y estado si NO es un cambio de contraseña obligatorio
        // O podemos guardarlo igual, pero el Guard impedirá la navegación.
        // En este caso, lo guardamos para que el usuario esté "autenticado" 
        // y pueda llamar al endpoint de cambiar password.
        this.saveToken(response.token);
        this.savePermissions(response.permisos);

        this.loggedIn.next(true);
        this.currentUserEmail.next(response.email);
        this.currentUserRole.next(response.roles ? response.roles[0] : null); 
        this.currentUserPermissions.next(response.permisos);
      })
    );
  }

  register(data: RegisterRequestDTO): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/register`, data);
  }

  // --- MÉTODO NUEVO AGREGADO ---
  cambiarPasswordInicial(nuevaPassword: string): Observable<any> {
    // El token se envía automáticamente por el interceptor (auth.interceptor.ts)
    // El cuerpo debe coincidir con el DTO del backend: { "nuevaPassword": "..." }
    return this.http.post(`${this.apiUrl}/cambiar-password-inicial`, { nuevaPassword });
  }
  // -----------------------------

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.PERMISSIONS_KEY);
    
    this.loggedIn.next(false);
    this.currentUserEmail.next(null);
    this.currentUserRole.next(null);
    this.currentUserPermissions.next([]);
    this.router.navigate(['/auth/login']);
  }

  private saveToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  private savePermissions(permissions: string[]): void {
    localStorage.setItem(this.PERMISSIONS_KEY, JSON.stringify(permissions));
  }
  
  private getPermissionsFromStorage(): string[] {
    const permissions = localStorage.getItem(this.PERMISSIONS_KEY);
    return permissions ? JSON.parse(permissions) : [];
  }

  hasToken(): boolean {
    const token = this.getToken();
    return !!token && !this.jwtHelper.isTokenExpired(token);
  }

  public getDecodedToken(): any | null {
    const token = this.getToken();
    if (token && this.hasToken()) { 
      try {
        return this.jwtHelper.decodeToken(token);
      } catch (e) {
        return null;
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

  public hasPermission(permissionName: string): boolean {
    return this.currentUserPermissions.value.includes(permissionName);
  }

  getApiUrlBase(): string {
    return API_URL;
  }
}