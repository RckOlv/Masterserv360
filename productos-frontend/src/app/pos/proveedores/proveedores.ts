import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
// Importar FormsModule para [(ngModel)]
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router'; // Mentor: Router para la navegación
import { ProveedorService } from '../../service/proveedor.service';
import { ProveedorDTO } from '../../models/proveedor.model';
import { CategoriaService } from '../../service/categoria.service'; 
import { CategoriaDTO } from '../../models/categoria.model'; 
import { mostrarToast } from '../../utils/toast';
import { HttpErrorResponse } from '@angular/common/http';
// import { AuthService } from '../../service/auth.service'; // Mentor: ELIMINADO

// --- Mentor: INICIO DE LA MODIFICACIÓN ---
// 1. Importar la nueva directiva de permisos
import { HasPermissionDirective } from '../../directives/has-permission.directive'; 
// --- Mentor: FIN DE LA MODIFICACIÓN ---

@Component({
  selector: 'app-proveedores',
  standalone: true,
  imports: [
    CommonModule, 
    ReactiveFormsModule, // Lo mantenemos por si usas un filtroForm
    FormsModule, 
    RouterModule,
    // --- Mentor: INICIO DE LA MODIFICACIÓN ---
    // 2. Añadir la directiva a los imports del componente
    HasPermissionDirective
    // --- Mentor: FIN DE LA MODIFICACIÓN ---
  ], 
  templateUrl: './proveedores.html',
  styleUrls: ['./proveedores.css']
})
export default class ProveedoresComponent implements OnInit {

  // Mentor: Quitamos FormBuilder ya que el formulario modal se movió
  private proveedorService = inject(ProveedorService);
  private categoriaService = inject(CategoriaService);
  // Mentor: ELIMINADA la inyección de AuthService
  // private authService = inject(AuthService); 
  private router = inject(Router); 

  // Estado
  proveedores: ProveedorDTO[] = [];
  proveedoresFiltrados: ProveedorDTO[] = [];
  categorias: CategoriaDTO[] = []; 
  terminoBusqueda: string = '';
  filtroEstado: string = 'ACTIVO';
  
  isLoading = false;
  errorMessage: string | null = null;
  
  // --- Mentor: INICIO DE LA MODIFICACIÓN ---
  // 3. 'isAdmin' se elimina por completo
  // public isAdmin = false;
  // --- Mentor: FIN DE LA MODIFICACIÓN ---

  constructor() {
    // El constructor queda vacío
  }

  ngOnInit() {
    // --- Mentor: INICIO DE LA MODIFICACIÓN ---
    // 4. Esta línea se elimina
    // this.isAdmin = this.authService.hasRole('ROLE_ADMIN');
    // --- Mentor: FIN DE LA MODIFICACIÓN ---
    
    this.listarProveedores();
    this.cargarCategorias();
  }

  /** 🔹 Carga categorías (para el badge de nombre) */
  cargarCategorias(): void {
    this.categoriaService.listarCategorias('ACTIVO').subscribe({
      next: (data) => this.categorias = data,
      error: (err: any) => console.error('Error al cargar categorías', err)
    });
  }

  /** 🔹 Carga proveedores desde el backend (filtrado por estado) */
  listarProveedores() {
    this.isLoading = true;
    this.errorMessage = null;
    
    this.proveedorService.listarProveedores(this.filtroEstado).subscribe({
      next: (data: ProveedorDTO[]) => {
        this.proveedores = data;
        this.filtrarLocalmente(); // Aplicar filtro de búsqueda local
        this.isLoading = false;
      },
      error: (err: HttpErrorResponse) => { 
        console.error('Error al listar proveedores:', err);
        this.handleError(err, 'cargar'); 
        this.isLoading = false;
      },
    });
  }

  /** 🔹 Se llama cuando cambian los filtros */
  aplicarFiltros(): void {
    this.listarProveedores();
  }

  /** 🔹 Filtra localmente por Razón Social o CUIT */
  filtrarLocalmente() {
    const termino = this.terminoBusqueda.toLowerCase().trim();
    if (!termino) {
      this.proveedoresFiltrados = [...this.proveedores];
    } else {
      this.proveedoresFiltrados = this.proveedores.filter((p) => 
        p.razonSocial.toLowerCase().includes(termino) || 
        (p.cuit && p.cuit.toLowerCase().includes(termino))
      );
    }
  }

  reiniciarFiltros() {
    this.terminoBusqueda = '';
    this.filtroEstado = 'ACTIVO'; 
    this.listarProveedores(); 
    mostrarToast('Filtros reiniciados');
  }

  /** Navega al formulario de nuevo o edición */
  navegarAFormulario(id?: number): void {
    if (id) {
      this.router.navigate(['/pos/proveedores/editar', id]);
    } else {
      this.router.navigate(['/pos/proveedores/nuevo']);
    }
  }

  eliminarProveedor(id?: number) {
    if (!id) return;
    if (confirm('¿Estás seguro de marcar este proveedor como INACTIVO?')) {
      this.isLoading = true;
      this.errorMessage = null;
      this.proveedorService.softDelete(id).subscribe({
        next: () => {
          this.listarProveedores(); 
          mostrarToast('Proveedor marcado como inactivo', 'warning');
        },
        error: (err: HttpErrorResponse) => {
          console.error('Error al eliminar proveedor:', err);
          this.handleError(err, 'eliminar');
          this.isLoading = false;
        },
      });
    }
  }

  reactivarProveedor(id?: number) {
     if (!id) return;
     if (confirm('¿Estás seguro de REACTIVAR este proveedor?')) {
       this.isLoading = true;
       this.proveedorService.reactivar(id).subscribe({
         next: () => {
           this.listarProveedores(); 
           mostrarToast('Proveedor reactivado', 'success');
         },
         error: (err: HttpErrorResponse) => {
           console.error('Error al reactivar proveedor:', err);
           this.handleError(err, 'reactivar');
           this.isLoading = false;
         },
       });
     }
   }
  
  obtenerNombreCategoria(catId: number): string {
    const categoria = this.categorias.find(c => c.id === catId);
    return categoria ? categoria.nombre : `ID: ${catId}`;
  }

  private handleError(err: HttpErrorResponse, context: string) {
    if (err.status === 403) {
      this.errorMessage = 'Acción no permitida: No tiene permisos de Administrador.';
    } else if (err.status === 500) {
      this.errorMessage = 'Ocurrió un error interno en el servidor.';
    } else {
      this.errorMessage = err.error?.message || `Error al ${context} el proveedor.`;
    }
    if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
  }
}