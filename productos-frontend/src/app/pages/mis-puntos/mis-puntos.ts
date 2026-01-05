import { Component, OnInit } from '@angular/core';
import { PuntosService } from '../../service/puntos.service';
import { CommonModule } from '@angular/common';
import { SaldoPuntos } from '../../models/saldo-puntos.model';
// ✅ IMPORTAR EL DTO CORRECTO
import { RecompensaDTO } from '../../models/recompensa.model'; 
import Swal from 'sweetalert2';

@Component({
  selector: 'app-mis-puntos',
  templateUrl: './mis-puntos.html',
  styleUrls: ['./mis-puntos.css'],
  imports: [CommonModule],
  standalone: true
})
export class MisPuntosComponent implements OnInit {

  saldo: SaldoPuntos | null = null;
  loading = true;

  constructor(private puntosService: PuntosService) { }

  ngOnInit(): void {
    this.cargarSaldo();
  }

  cargarSaldo() {
    this.loading = true;
    this.puntosService.getMiSaldo().subscribe({
      next: (data) => {
        this.saldo = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error cargando saldo:', err);
        this.loading = false;
      }
    });
  }

  // ✅ CORRECCIÓN: Cambiamos el tipo a RecompensaDTO
  confirmarCanje(recompensa: RecompensaDTO) {
    
    // ✅ VALIDACIÓN DE SEGURIDAD:
    // TypeScript sabe que 'id' es opcional en el DTO, así que verificamos que exista.
    if (!recompensa.id) {
        console.error("Error: La recompensa seleccionada no tiene ID válido.");
        return;
    }

    // Guardamos el ID en una variable para asegurar el tipo 'number'
    const idRecompensa = recompensa.id;

    Swal.fire({
      title: '¿Estás seguro?',
      text: `Vas a canjear "${recompensa.descripcion}" por ${recompensa.puntosRequeridos} puntos.`,
      icon: 'question',
      showCancelButton: true,
      confirmButtonColor: '#3085d6',
      cancelButtonColor: '#d33',
      confirmButtonText: 'Sí, canjear',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        // Ahora pasamos el ID seguro
        this.realizarCanje(idRecompensa); 
      }
    });
  }

  realizarCanje(id: number) {
    this.puntosService.canjearPuntos(id).subscribe({
      next: (response) => {
        Swal.fire(
          '¡Canje Exitoso!',
          `Tu código es: <b>${response.codigo}</b>. Úsalo en tu próxima compra.`,
          'success'
        );
        this.cargarSaldo();
      },
      error: (err) => {
        Swal.fire('Error', err.error?.message || 'No se pudo procesar el canje.', 'error');
      }
    });
  }
}