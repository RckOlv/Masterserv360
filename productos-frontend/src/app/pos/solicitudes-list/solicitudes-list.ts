import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { SolicitudService } from '../../service/solicitud.service';
import { SolicitudProducto } from '../../models/solicitud.model';
import { mostrarToast } from '../../utils/toast';
import { HasPermissionDirective } from '../../directives/has-permission.directive';

@Component({
  selector: 'app-solicitudes-list',
  standalone: true,
  imports: [CommonModule, HasPermissionDirective,RouterLink],
  templateUrl: './solicitudes-list.html',
  styles: [`
    .card-procesada { border-left: 4px solid #198754 !important; opacity: 0.7; }
    .card-pendiente { border-left: 4px solid #ffc107 !important; }
  `]
})
export default class SolicitudesListComponent implements OnInit {
  
  private solicitudService = inject(SolicitudService);
  
  solicitudes: SolicitudProducto[] = [];
  isLoading = true;

  ngOnInit(): void {
    this.cargarSolicitudes();
  }

  cargarSolicitudes(): void {
    this.isLoading = true;
    this.solicitudService.getAll().subscribe({
      next: (data) => {
        this.solicitudes = data;
        this.isLoading = false;
      },
      error: () => {
        mostrarToast('Error cargando solicitudes.', 'danger');
        this.isLoading = false;
      }
    });
  }

  procesar(sol: SolicitudProducto): void {
    if (sol.procesado) return;
    
    this.solicitudService.marcarProcesada(sol.id).subscribe({
      next: () => {
        sol.procesado = true;
        mostrarToast('Solicitud archivada.', 'success');
      },
      error: () => mostrarToast('Error al procesar.', 'danger')
    });
  }

  eliminar(id: number): void {
    if(!confirm('¿Borrar esta solicitud permanentemente?')) return;
    
    this.solicitudService.eliminar(id).subscribe({
      next: () => {
        this.solicitudes = this.solicitudes.filter(s => s.id !== id);
        mostrarToast('Solicitud eliminada.', 'success');
      },
      error: () => mostrarToast('Error al eliminar.', 'danger')
    });
  }
}