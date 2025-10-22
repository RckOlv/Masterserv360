import { Component } from '@angular/core';
import { NgForm,FormsModule } from '@angular/forms';
import { UsuarioService } from '../../service/usuario.service';
import { Router } from '@angular/router';
import { Usuario } from '../../models/usuario.model';
import { CommonModule } from '@angular/common';


@Component({
  selector: 'app-registro-cliente',
  templateUrl: './reg-cli.html',
  styleUrls: ['./reg-cli.css'],
  imports: [CommonModule,FormsModule]
})
export class RegistroClienteComponent {

  usuario: Usuario = {
    nombre: '',
    apellido: '',
    documento: '',
    email: '',
    password: ''
  };

  constructor(private usuarioService: UsuarioService, public router: Router) {}

  registrarUsuario(form: NgForm): void {
    if (form.valid) {
      this.usuarioService.registrarCliente(this.usuario).subscribe({
        next: () => {
          alert('Registro exitoso');
          this.router.navigate(['/login']);
        },
        error: () => alert('Error al registrar el usuario')
      });
    }
  }
}
