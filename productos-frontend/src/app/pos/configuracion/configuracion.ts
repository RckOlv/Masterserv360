import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common'; // 👈 Importante para *ngIf, *ngFor, ngClass
import { FormsModule } from '@angular/forms';   // 👈 Importante para [(ngModel)]
import { ConfiguracionService, EmpresaConfig } from '../../service/configuracion.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-configuracion',
  standalone: true, // 👈 Esto le dice a Angular que es Standalone
  imports: [
    CommonModule, 
    FormsModule     // 👈 Aquí habilitamos el uso de formularios en este componente
  ],
  templateUrl: './configuracion.html',
  styleUrls: ['./configuracion.css']
})
export class ConfiguracionComponent implements OnInit {

  config: EmpresaConfig = {
    razonSocial: '',
    nombreFantasia: '',
    cuit: '',
    direccion: '',
    telefono: '',
    emailContacto: '',
    sitioWeb: '',
    logoUrl: '',
    colorPrincipal: '#E41E26', // Rojo por defecto
    piePaginaPresupuesto: ''
  };

  isLoading = false;

  constructor(private configService: ConfiguracionService) { }

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos() {
    this.isLoading = true;
    this.configService.getConfiguracion().subscribe({
      next: (data) => {
        // Si viene null (primera vez), mantenemos los defaults, si no, asignamos data
        if (data) {
             this.config = data;
        }
        this.isLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.isLoading = false;
        Swal.fire('Error', 'No se pudo cargar la configuración', 'error');
      }
    });
  }

  guardar() {
    this.isLoading = true;
    this.configService.updateConfiguracion(this.config).subscribe({
      next: (data) => {
        this.config = data;
        this.isLoading = false;
        Swal.fire({
          title: '¡Guardado!',
          text: 'Los datos de la empresa se actualizaron correctamente.',
          icon: 'success',
          confirmButtonColor: '#E41E26'
        });
      },
      error: (err) => {
        console.error(err);
        this.isLoading = false;
        Swal.fire('Error', 'No se pudieron guardar los cambios', 'error');
      }
    });
  }
}