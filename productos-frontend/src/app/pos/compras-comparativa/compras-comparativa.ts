import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ComprasService } from '../../service/compras.service';
import { ResumenProductoCompra, DetalleComparativa } from '../../models/compras.model';
import { HasPermissionDirective } from '../../directives/has-permission.directive';

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

    // Abrir Modal
    const el = document.getElementById('modalComparativa');
    if (el) {
      this.modalInstance = new bootstrap.Modal(el);
      this.modalInstance.show();
    }

    // Cargar Datos
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
  
  cerrarModal() {
    if (this.modalInstance) {
      this.modalInstance.hide();
    }
  }
}