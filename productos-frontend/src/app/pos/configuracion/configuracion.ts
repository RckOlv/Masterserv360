import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common'; 
import { FormsModule } from '@angular/forms'; 
import { ConfiguracionService, EmpresaConfig } from '../../service/configuracion.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-configuracion',
  standalone: true, 
  imports: [
    CommonModule, 
    FormsModule     
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
    colorPrincipal: '#E41E26', 
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

  // 👇 MÉTODO NUEVO: Convierte la imagen a Texto Base64
  onFileSelected(event: any) {
    const file: File = event.target.files[0];
    
    if (file) {
      // 1. Validar tamaño (Máximo 800KB para no saturar la BD)
      if (file.size > 800000) {
        Swal.fire('Imagen muy pesada', 'Por favor usa una imagen menor a 800KB', 'error');
        return;
      }

      const reader = new FileReader();
      
      // 2. Cuando termine de leer, guardar el string en la variable
      reader.onload = (e: any) => {
        this.config.logoUrl = e.target.result; 
      };

      // 3. Leer archivo
      reader.readAsDataURL(file);
    }
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