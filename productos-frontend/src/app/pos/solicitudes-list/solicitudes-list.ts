import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SolicitudService, ListaEsperaItem } from '../../service/solicitud.service';
import { HasPermissionDirective } from '../../directives/has-permission.directive';
import { mostrarToast, confirmarAccion } from '../../utils/toast'; // ✅ AÑADIDO confirmarAccion

@Component({
  selector: 'app-solicitudes-list',
  standalone: true,
  imports: [CommonModule, HasPermissionDirective],
  templateUrl: './solicitudes-list.html',
  styleUrls: ['./solicitudes-list.css']
})
export default class SolicitudesListComponent implements OnInit {

  private solicitudService = inject(SolicitudService);

  public solicitudes: ListaEsperaItem[] = [];
  public isLoading = true;

  ngOnInit() {
    this.cargarDatos();
  }

  cargarDatos() {
    this.isLoading = true;
    this.solicitudService.getAll().subscribe({
      next: (data) => {
        this.solicitudes = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.isLoading = false;
      }
    });
  }

  /** ✅ ELIMINAR SOLICITUD MIGRADO */
  eliminar(id: number) {
    confirmarAccion(
      'Eliminar de la lista',
      '¿Borrar de la lista?'
    ).then((confirmado) => {
      if (confirmado) {
        this.solicitudService.eliminar(id).subscribe({
          next: () => {
            mostrarToast('Eliminado', 'success');
            this.cargarDatos();
          },
          error: () => mostrarToast('Error al eliminar', 'danger')
        });
      }
    });
  }
}