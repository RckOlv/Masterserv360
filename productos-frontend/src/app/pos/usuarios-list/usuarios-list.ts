import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common'; // Reemplaza NgFor, NgIf
import { RouterModule } from '@angular/router';
import { UsuarioService } from '../../service/usuario.service'; // Asegúrate que la ruta es correcta
import { UsuarioDTO } from '../../models/usuario.model'; // <-- CAMBIO: Usa DTO
// import { AuthService } from '../../service/auth.service'; // Comentado si no se usa para permisos
// import RegistroAdminComponent from '../../layouts/admin-layout/registro/registro'; // <-- CAMBIO: Importación default

@Component({
  selector: 'app-usuario-list',
  standalone: true,
  // 1. Simplificar imports: CommonModule ya incluye NgFor/NgIf
  // 2. Quitar RegistroAdminComponent si no se usa en el template
  imports: [
    CommonModule, 
    RouterModule 
    // RegistroAdminComponent // <-- Quitar si no usas <app-registro-admin> en el HTML
  ], 
  templateUrl: './usuarios-list.html',
  styleUrls: ['./usuarios-list.css']
})
export default class UsuarioListComponent implements OnInit {
  
  // 3. Usar el tipo DTO
  usuarios: UsuarioDTO[] = []; 
  // mostrarFormulario = false; // Ya no parece usarse según el código

  // 4. Inyectar UsuarioService (AuthService comentado si no se usa)
  private usuarioService = inject(UsuarioService); 
  // public authService = inject(AuthService); // <-- Comentado

  constructor() {} // El constructor puede estar vacío

  ngOnInit() {
    this.cargarUsuarios();
  }

  cargarUsuarios() {
    // 5. Asumir que UsuarioService devuelve UsuarioDTO[]
    this.usuarioService.listarUsuarios().subscribe({ 
      next: (data) => this.usuarios = data,
      error: (err: any) => { // Añadir tipo 'any' al error
        console.error('Error al cargar usuarios:', err);
        // Aquí podrías usar mostrarToast('Error al cargar usuarios', 'danger');
      }
    });
  }

  eliminarUsuario(id: number | undefined) { // Añadir tipo 'undefined'
    // 6. Verificar el ID antes de usarlo
    if (!id) {
      console.error('Intento de eliminar usuario sin ID.');
      return; 
    }
    if (confirm('¿Seguro que deseas marcar este usuario como inactivo?')) { // Cambiar mensaje a soft delete
      // 7. Asumir que UsuarioService tiene softDelete y devuelve algo
      this.usuarioService.softDelete(id).subscribe({ 
        next: () => {
          // Recargar la lista para reflejar el cambio
          this.cargarUsuarios(); 
          // mostrarToast('Usuario marcado como inactivo', 'warning');
        },
        error: (err: any) => { // Añadir tipo 'any'
           console.error('Error al eliminar usuario:', err);
           // mostrarToast(err.error?.message || 'Error al eliminar usuario', 'danger');
        }
      });
    }
  }

  // Ya no parece necesario si el formulario es un componente separado
  // alternarFormulario() {
  //  this.mostrarFormulario = !this.mostrarFormulario;
  // }
}