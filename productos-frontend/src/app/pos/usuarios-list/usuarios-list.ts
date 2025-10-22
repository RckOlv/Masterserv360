import { Component, OnInit } from '@angular/core';
import { CommonModule, NgFor, NgIf } from '@angular/common'; 
import { RouterModule } from '@angular/router';
import { UsuarioService } from '../../service/usuario.service';
import { Usuario } from '../../models/usuario.model';
import { AuthService } from '../../service/auth.service';
import { RegistroAdminComponent } from '../../layouts/admin-layout/registro/registro';


@Component({
  selector: 'app-usuario-list',
  standalone: true,
  imports: [CommonModule, NgFor, NgIf, RouterModule, RegistroAdminComponent],
  templateUrl: './usuarios-list.html',
  styleUrls: ['./usuarios-list.css']
})
export class UsuarioListComponent implements OnInit {
  usuarios: Usuario[] = [];
  mostrarFormulario = false;

  constructor(private usuarioService: UsuarioService, public authService: AuthService) {}

  ngOnInit() {
    this.cargarUsuarios();
  }

  cargarUsuarios() {
    this.usuarioService.listarUsuarios().subscribe({
      next: (data) => this.usuarios = data,
      error: (err) => console.error(err)
    });
  }

  eliminarUsuario(id: number) {
    if (confirm('¿Seguro que deseas eliminar este usuario?')) {
      this.usuarioService.eliminarUsuario(id).subscribe(() => this.cargarUsuarios());
    }
  }

  alternarFormulario() {
    this.mostrarFormulario = !this.mostrarFormulario;
  }
}
