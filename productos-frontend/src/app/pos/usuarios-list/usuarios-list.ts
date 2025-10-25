import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms'; // Importar Forms
import { UsuarioService } from '../../service/usuario.service';
import { UsuarioDTO } from '../../models/usuario.model';
import { UsuarioFiltroDTO } from '../../models/usuario-filtro.model';
import { Page } from '../../models/page.model';
import { RolService } from '../../service/rol.service'; // Para el dropdown
import { RolDTO } from '../../models/rol.model';
import { mostrarToast } from '../../utils/toast';

@Component({
  selector: 'app-usuario-list',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule], // Añadir ReactiveFormsModule
  templateUrl: './usuarios-list.html',
  styleUrls: ['./usuarios-list.css']
})
export default class UsuarioListComponent implements OnInit {
  
  // Estado
  usuariosPage: Page<UsuarioDTO> | null = null;
  filtroForm: FormGroup;
  roles: RolDTO[] = []; // Para el dropdown
  currentPage = 0;
  pageSize = 10;
  errorMessage: string | null = null;
  isLoading = false;
  
  // Lista de estados para el filtro
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
    // Inicializamos el formulario de filtros
    this.filtroForm = this.fb.group({
      nombreOEmail: [''],
      documento: [''],
      rolId: [null],
      estado: ['ACTIVO'] // Por defecto filtramos activos
    });
  }

  ngOnInit() {
    this.cargarRoles();
    this.cargarUsuarios(); // Carga inicial (solo Activos)
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
    
    // Si el estado es "null" (Todos), lo enviamos como null
    if (filtro.estado === null) {
      filtro.estado = null;
    }

    this.usuarioService.filtrarUsuarios(filtro, this.currentPage, this.pageSize).subscribe({ 
      next: (data: Page<UsuarioDTO>) => { 
        this.usuariosPage = data;
        this.isLoading = false;
      },
      error: (err: any) => { 
        console.error('Error al cargar usuarios:', err);
        this.errorMessage = "Error al cargar usuarios.";
        this.isLoading = false;
      }
    });
  }

  aplicarFiltros(): void {
    this.currentPage = 0; // Resetea a la primera página
    this.cargarUsuarios();
  }

  limpiarFiltros(): void {
    this.filtroForm.reset({
      nombreOEmail: '',
      documento: '',
      rolId: null,
      estado: 'ACTIVO' // Vuelve al estado por defecto
    });
    this.aplicarFiltros();
  }

  eliminarUsuario(id: number | undefined) { 
    if (!id) return;
    if (confirm('¿Seguro que deseas marcar este usuario como INACTIVO?')) { 
      this.usuarioService.softDelete(id).subscribe({ 
        next: () => {
          mostrarToast('Usuario marcado como inactivo', 'warning');
          this.cargarUsuarios(); // Recarga la lista
        },
        error: (err: any) => { 
           console.error('Error al eliminar usuario:', err);
           this.errorMessage = err.error?.message || 'Error al eliminar usuario.';
           if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
        }
      });
    }
  }
  
  reactivarUsuario(id: number | undefined) {
    if (!id) return;
    if (confirm('¿Seguro que deseas REACTIVAR este usuario?')) {
      this.usuarioService.reactivar(id).subscribe({
        next: () => {
          mostrarToast('Usuario reactivado correctamente', 'success');
          this.cargarUsuarios(); // Recarga la lista
        },
        error: (err: any) => {
           console.error('Error al reactivar usuario:', err);
           this.errorMessage = err.error?.message || 'Error al reactivar usuario.';
           if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
        }
      });
    }
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
}