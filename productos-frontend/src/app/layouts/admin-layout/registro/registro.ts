import { Component, OnInit } from '@angular/core';
import { NgForm,FormsModule } from '@angular/forms';
import { UsuarioService } from '../../../service/usuario.service';
import { RolService } from '../../../service/rol.service';
import { Router } from '@angular/router';
import { Usuario } from '../../../models/usuario.model';
import { Rol } from '../../../models/rol.model';
import { CommonModule } from '@angular/common';

@Component({
  standalone: true,
  selector: 'app-registro-admin',
  templateUrl: './registro.html',
  styleUrls: ['./registro.css'],
  imports: [FormsModule,CommonModule]
})
export class RegistroAdminComponent implements OnInit {

  usuario: Usuario = {
    nombre: '',
    apellido: '',
    documento: '',
    email: '',
    password: '',
    rol: {
      id: 0, nombre: '',
      permisos: []
    }
  };

  roles: Rol[] = [];

  constructor(
    private usuarioService: UsuarioService,
    private rolService: RolService,
    public router: Router
  ) {}

  ngOnInit(): void {
    this.rolService.listarRoles().subscribe({
      next: (roles) => this.roles = roles,
      error: () => alert('Error al cargar los roles')
    });
  }

  registrarUsuario(form: NgForm): void {
    if (form.valid) {
      this.usuarioService.registrarUsuario(this.usuario).subscribe({
        next: () => {
          alert('Usuario creado con éxito');
          this.router.navigate(['/admin/usuarios']);
        },
        error: () => alert('Error al crear el usuario')
      });
    }
  }
}
