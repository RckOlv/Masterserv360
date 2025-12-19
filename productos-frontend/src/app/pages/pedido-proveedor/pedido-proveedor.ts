import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common'; 
import { FormsModule } from '@angular/forms'; 
import { PublicService } from '../../service/public.service';
import Swal from 'sweetalert2'; 

@Component({
  selector: 'app-pedido-proveedor',
  standalone: true,
  imports: [CommonModule, FormsModule], 
  templateUrl: './pedido-proveedor.html',
  styleUrls: ['./pedido-proveedor.css']
})
export class PedidoProveedorComponent implements OnInit {
  
  private route = inject(ActivatedRoute);
  private publicService = inject(PublicService);

  token: string = '';
  pedido: any = null;
  loading: boolean = true;
  error: string = '';
  
  fechaEntrega: string = '';
  enviando: boolean = false;

  // --- NUEVA VARIABLE PARA LA FECHA MÍNIMA ---
  minDate: string = '';

  ngOnInit(): void {
    // 1. Calcular fecha de hoy en formato YYYY-MM-DD
    const hoy = new Date();
    this.minDate = hoy.toISOString().split('T')[0];

    // 2. Obtener token y cargar
    this.token = this.route.snapshot.paramMap.get('token') || '';

    if (this.token) {
      this.cargarDatosPedido();
    } else {
      this.error = 'Enlace inválido o incompleto.';
      this.loading = false;
    }
  }

  cargarDatosPedido() {
    this.publicService.obtenerPedidoPorToken(this.token).subscribe({
      next: (data: any) => {
        this.pedido = data;
        
        // Inicializar precios (ahora vendrán en 0 desde el backend)
        if (this.pedido.detalles) {
            this.pedido.detalles.forEach((d: any) => {
                if (!d.precio) d.precio = 0;
            });
        }

        if (this.pedido.estado !== 'PENDIENTE') {
            this.error = 'Este pedido ya fue gestionado anteriormente.';
            this.pedido = null; 
        }

        this.loading = false;
      },
      error: (err: any) => {
        console.error(err);
        this.error = 'No se pudo cargar la información del pedido. El enlace puede haber expirado.';
        this.loading = false;
      }
    });
  }

  confirmarPedido() {
    if (!this.fechaEntrega) {
        Swal.fire('Falta información', 'Por favor seleccione la fecha estimada de entrega.', 'warning');
        return;
    }

    // --- VALIDACIÓN EXTRA DE SEGURIDAD ---
    if (this.fechaEntrega < this.minDate) {
        Swal.fire('Fecha inválida', 'La fecha de entrega no puede ser anterior a hoy.', 'error');
        return;
    }

    this.enviando = true;

    const dto = {
        fechaEntrega: this.fechaEntrega,
        items: this.pedido.detalles.map((d: any) => ({
            productoId: d.productoId,
            nuevoPrecio: d.precio
        }))
    };

    this.publicService.confirmarPedidoProveedor(this.token, dto).subscribe({
        next: () => {
            Swal.fire('¡Confirmado!', 'El pedido ha sido procesado correctamente. Gracias.', 'success');
            this.pedido = null; 
            this.error = 'Pedido confirmado exitosamente. Muchas gracias.';
        },
        error: (err: any) => {
            console.error(err);
            Swal.fire('Error', 'Ocurrió un problema al confirmar. Intente nuevamente.', 'error');
            this.enviando = false;
        }
    });
  }
}