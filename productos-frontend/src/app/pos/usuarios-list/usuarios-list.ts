import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms'; 
import { UsuarioService } from '../../service/usuario.service';
import { UsuarioDTO } from '../../models/usuario.model';
import { UsuarioFiltroDTO } from '../../models/usuario-filtro.model';
import { Page } from '../../models/page.model';
import { RolService } from '../../service/rol.service'; 
import { RolDTO } from '../../models/rol.model';
import { mostrarToast, confirmarAccion } from '../../utils/toast'; // ✅ AÑADIDO confirmarAccion

import { HasPermissionDirective } from '../../directives/has-permission.directive'; 
import { HttpErrorResponse } from '@angular/common/http'; 

@Component({
  selector: 'app-usuario-list',
  standalone: true,
  imports: [
    CommonModule, 
    RouterModule, 
    ReactiveFormsModule,
    HasPermissionDirective
  ], 
  templateUrl: './usuarios-list.html',
  styleUrls: ['./usuarios-list.css']
})
export default class UsuarioListComponent implements OnInit {
  
  // Estado
  usuariosPage: Page<UsuarioDTO> | null = null;
  filtroForm: FormGroup;
  roles: RolDTO[] = []; 
  currentPage = 0;
  pageSize = 10;
  errorMessage: string | null = null;
  isLoading = false;
  
  estadosUsuario = [
    { valor: 'ACTIVO', texto: 'Activos' },
    { valor: 'INACTIVO', texto: 'Inactivos' },
    { valor: 'PENDIENTE', texto: 'Pendientes' },
    { valor: 'BLOQUEADO', texto: 'Bloqueados' }
  ];

  // Inyección
  private usuarioService = inject(UsuarioService);
  private rolService = inject(RolService);
  private fb = inject(FormBuilder);

  constructor() {
    this.filtroForm = this.fb.group({
      nombreOEmail: [''],
      documento: [''],
      rolId: [null],
      estado: ['ACTIVO'] 
    });
  }

  ngOnInit() {
    this.cargarRoles();
    this.cargarUsuarios(); 
  }

  cargarRoles(): void {
    this.rolService.listarRoles().subscribe({
      next: (data) => this.roles = data,
      error: (err: any) => console.error('Error al cargar roles', err)
    });
  }

  cargarUsuarios() {
    this.isLoading = true;
    this.errorMessage = null;
    const filtro = this.filtroForm.value as UsuarioFiltroDTO;
    
    if (filtro.estado === null) {
      filtro.estado = null;
    }

    this.usuarioService.filtrarUsuarios(filtro, this.currentPage, this.pageSize).subscribe({ 
      next: (data: Page<UsuarioDTO>) => { 
        this.usuariosPage = data;
        this.isLoading = false;
      },
      error: (err: HttpErrorResponse) => {
        console.error('Error al cargar usuarios:', err);
        this.handleError(err, 'cargar'); 
        this.isLoading = false;
      }
    });
  }

  aplicarFiltros(): void {
    this.currentPage = 0; 
    this.cargarUsuarios();
  }

  limpiarFiltros(): void {
    this.filtroForm.reset({
      nombreOEmail: '',
      documento: '',
      rolId: null,
      estado: 'ACTIVO' 
    });
    this.aplicarFiltros();
  }

  /** ✅ ELIMINAR/DESACTIVAR USUARIO MIGRADO */
  eliminarUsuario(id: number | undefined) { 
    if (!id) return;
    
    confirmarAccion(
      'Desactivar Usuario',
      '¿Seguro que deseas marcar este usuario como INACTIVO?'
    ).then((confirmado) => {
      if (confirmado) {
        this.usuarioService.softDelete(id).subscribe({ 
          next: () => {
            mostrarToast('Usuario marcado como inactivo', 'warning');
            this.cargarUsuarios(); 
          },
          error: (err: HttpErrorResponse) => {
             console.error('Error al eliminar usuario:', err);
             this.handleError(err, 'eliminar');
          }
        });
      }
    });
  }
  
  /** ✅ REACTIVAR USUARIO MIGRADO */
  reactivarUsuario(id: number | undefined) {
    if (!id) return;

    confirmarAccion(
      'Reactivar Usuario',
      '¿Seguro que deseas REACTIVAR este usuario?'
    ).then((confirmado) => {
      if (confirmado) {
        this.usuarioService.reactivar(id).subscribe({
          next: () => {
            mostrarToast('Usuario reactivado correctamente', 'success');
            this.cargarUsuarios(); 
          },
          error: (err: HttpErrorResponse) => {
             console.error('Error al reactivar usuario:', err);
             this.handleError(err, 'reactivar');
          }
        });
      }
    });
  }

  // --- Métodos de Paginación ---
  irAPagina(pageNumber: number): void {
    if (pageNumber >= 0 && (!this.usuariosPage || pageNumber < this.usuariosPage.totalPages)) {
      this.currentPage = pageNumber;
      this.cargarUsuarios();
    }
  }

  get totalPaginas(): number {
    return this.usuariosPage ? this.usuariosPage.totalPages : 0;
  }
  
  // --- Mentor: Helper de Errores ---
  private handleError(err: HttpErrorResponse, context: string) {
    if (err.status === 403) {
      this.errorMessage = 'Acción no permitida: No tiene permisos.';
    } else if (err.status === 500) {
      this.errorMessage = 'Ocurrió un error interno en el servidor.';
    } else {
      this.errorMessage = err.error?.message || `Error al ${context} el usuario.`;
    }
    if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
  }
}