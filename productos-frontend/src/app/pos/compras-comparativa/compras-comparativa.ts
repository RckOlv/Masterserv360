import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ComprasService } from '../../service/compras.service';
import { ResumenProductoCompra, DetalleComparativa } from '../../models/compras.model';
import { HasPermissionDirective } from '../../directives/has-permission.directive';
import { mostrarToast } from '../../utils/toast';
import Swal from 'sweetalert2';

declare var bootstrap: any;

@Component({
  selector: 'app-compras-comparativa',
  standalone: true,
  imports: [CommonModule, RouterLink, HasPermissionDirective],
  templateUrl: './compras-comparativa.html',
  styleUrls: ['./compras-comparativa.css']
})
export default class ComprasComparativaComponent implements OnInit {

  private comprasService = inject(ComprasService);

  public productos: ResumenProductoCompra[] = [];
  public isLoading = true;

  // Variables para el Modal
  public detalleProducto: ResumenProductoCompra | null = null;
  public comparativa: DetalleComparativa[] = [];
  public isLoadingDetalle = false;
  private modalInstance: any;

  // ✅ Mapa de selecciones: { productoId: itemCotizacionId }
  public selecciones: Map<number, number> = new Map();

  ngOnInit() {
    this.cargarProductos();
  }

  cargarProductos() {
    this.isLoading = true;
    this.comprasService.getProductosCotizados().subscribe({
      next: (data) => {
        this.productos = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error al cargar productos:', err);
        this.isLoading = false;
      }
    });
  }

  abrirComparativa(prod: ResumenProductoCompra) {
    this.detalleProducto = prod;
    this.comparativa = [];
    this.isLoadingDetalle = true;

    const el = document.getElementById('modalComparativa');
    if (el) {
      this.modalInstance = new bootstrap.Modal(el);
      this.modalInstance.show();
    }

    this.comprasService.getComparativaProducto(prod.productoId).subscribe({
      next: (data) => {
        this.comparativa = data;
        this.isLoadingDetalle = false;
      },
      error: (err) => {
        console.error(err);
        this.isLoadingDetalle = false;
      }
    });
  }

  // ✅ Seleccionar un ganador desde el modal
  seleccionarGanador(item: DetalleComparativa) {
    if (this.detalleProducto) {
      this.selecciones.set(this.detalleProducto.productoId, item.itemCotizacionId);
      mostrarToast('Proveedor seleccionado para este producto', 'success');
      this.cerrarModal();
    }
  }

  // ✅ Verificar si un producto ya tiene un ganador elegido
  getSeleccionado(productoId: number): number | undefined {
    return this.selecciones.get(productoId);
  }

  // ✅ PROCESO FINAL: Generar todos los pedidos agrupados
  procesarCompraMasiva() {
    const idsParaComprar = Array.from(this.selecciones.values());

    if (idsParaComprar.length === 0) {
      Swal.fire('Atención', 'Selecciona al menos un proveedor ganador para algún producto.', 'warning');
      return;
    }

    Swal.fire({
      title: '¿Confirmar Pedidos?',
      text: `Se generarán pedidos automáticos para ${idsParaComprar.length} productos.`,
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Sí, generar pedidos',
      cancelButtonText: 'Revisar más',
      confirmButtonColor: '#ffc107',
      background: '#1a1a1a',
      color: '#ffffff'
    }).then((result) => {
      if (result.isConfirmed) {
        this.isLoading = true;
        this.comprasService.generarPedidosMasivos(idsParaComprar).subscribe({
          next: (res) => {
            Swal.fire('¡Éxito!', res.mensaje, 'success');
            this.selecciones.clear();
            this.cargarProductos();
          },
          error: (err) => {
            console.error(err);
            Swal.fire('Error', 'Hubo un problema al generar los pedidos.', 'error');
            this.isLoading = false;
          }
        });
      }
    });
  }
  
  cerrarModal() {
    if (this.modalInstance) {
      this.modalInstance.hide();
    }
  }
}