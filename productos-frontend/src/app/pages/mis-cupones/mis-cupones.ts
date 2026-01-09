import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common'; 
import { RouterModule } from '@angular/router'; 
import { ClienteService } from '../../service/cliente.service';
import { CuponDTO } from '../../models/cupon.model';

@Component({
  selector: 'app-mis-cupones',
  standalone: true, // <--- ARQUITECTURA MODERNA
  imports: [CommonModule, RouterModule], 
  templateUrl: './mis-cupones.html',
  styleUrls: ['./mis-cupones.css']
})
export class MisCuponesComponent implements OnInit {

  cuponesVigentes: CuponDTO[] = [];
  cuponesHistorial: CuponDTO[] = [];
  loading: boolean = true;

  constructor(private clienteService: ClienteService) {}

  ngOnInit(): void {
    this.cargarCupones();
  }

  cargarCupones() {
    this.loading = true;
    this.clienteService.getMisCupones().subscribe({
      next: (data) => {
        // Separamos para mejor UX: Lo que sirve vs. Lo viejo
        // Ajusta los strings según lo que devuelva tu Enum de Java exactamente
        this.cuponesVigentes = data.filter(c => c.estado === 'VIGENTE' || c.estado === 'DISPONIBLE');
        this.cuponesHistorial = data.filter(c => c.estado !== 'VIGENTE' && c.estado !== 'DISPONIBLE');
        this.loading = false;
      },
      error: (err) => {
        console.error('Error al cargar cupones', err);
        this.loading = false;
      }
    });
  }

  copiarCodigo(codigo: string) {
    navigator.clipboard.writeText(codigo).then(() => {
      // Si tienes un ToastService, úsalo aquí. Por ahora, un alert simple:
      alert('¡Código copiado al portapapeles!');
    });
  }
}