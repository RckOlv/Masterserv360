import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common'; // Para *ngIf, pipes
import { ActivatedRoute, RouterLink } from '@angular/router'; // Para leer ID de URL y link Volver
import { VentaService } from '../../service/venta.service';
import { VentaDTO } from '../../models/venta.model';
import { mostrarToast } from '../../utils/toast';

@Component({
  selector: 'app-venta-detalle',
  standalone: true,
  imports: [CommonModule, RouterLink], // Importar CommonModule y RouterLink
  templateUrl: './venta-detalle.html',
  styleUrls: ['./venta-detalle.css']
})
export default class VentaDetalleComponent implements OnInit {

  // --- Inyección de Dependencias ---
  private route = inject(ActivatedRoute); // Para leer parámetros de la URL
  private ventaService = inject(VentaService);

  // --- Estado del Componente ---
  public venta: VentaDTO | null = null;
  public isLoading = true; // Empieza cargando
  public errorMessage: string | null = null;
  private ventaId: number | null = null;

  constructor() { }

  ngOnInit(): void {
    // Obtener el ID de la venta desde los parámetros de la ruta
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.ventaId = +idParam; // El '+' convierte string a number
      if (!isNaN(this.ventaId)) {
        this.cargarDetalleVenta();
      } else {
        this.showErrorAndStopLoading("ID de venta inválido en la URL.");
      }
    } else {
      this.showErrorAndStopLoading("No se proporcionó ID de venta en la URL.");
    }
  }

  /** Llama al servicio para obtener los detalles de la venta */
  cargarDetalleVenta(): void {
    this.isLoading = true;
    this.errorMessage = null;

    this.ventaService.getVentaById(this.ventaId!).subscribe({ // Usamos '!' porque ya validamos que no es null
      next: (data) => {
        this.venta = data;
        this.isLoading = false;
        // Validar si la venta realmente tiene detalles (backend debería devolverlos)
        if (!data.detalles || data.detalles.length === 0) {
             console.warn("La venta obtenida no tiene detalles asociados.");
             // Podrías mostrar un mensaje específico si esto no debería pasar
        }
      },
      error: (err) => {
        console.error("Error al cargar detalle de venta:", err);
        const msg = err.status === 404 ? "Venta no encontrada." : "No se pudo cargar el detalle de la venta.";
        this.showErrorAndStopLoading(msg);
      }
    });
  }

  /** Helper para mostrar errores */
  private showErrorAndStopLoading(message: string): void {
      this.errorMessage = message;
      mostrarToast(this.errorMessage, 'danger');
      this.isLoading = false;
  }

}