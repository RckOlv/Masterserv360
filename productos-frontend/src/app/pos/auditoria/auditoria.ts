import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuditoriaService } from '../../service/auditoria.service';
import { Auditoria } from '../../models/auditoria.model';
import { Page } from '../../models/page.model';
import { HasPermissionDirective } from '../../directives/has-permission.directive';

declare var bootstrap: any;

@Component({
  selector: 'app-auditoria-list',
  standalone: true,
  imports: [CommonModule, FormsModule, HasPermissionDirective],
  templateUrl: './auditoria.html',
  styleUrls: ['./auditoria.css']
})
export default class AuditoriaListComponent implements OnInit {
  
  private auditoriaService = inject(AuditoriaService);
  
  page: Page<Auditoria> | null = null;
  isLoading = true;
  currentPage = 0;
  pageSize = 20;

  // --- FILTROS SIMPLIFICADOS ---
  mostrarFiltros = false;
  filtro: any = {
    accion: '', 
    entidad: '',
    fechaDesde: null,
    fechaHasta: null
  };
  // -----------------------------

  selectedLog: Auditoria | null = null;
  datosAnteriores: any[] = [];
  datosNuevos: any[] = [];
  private detalleModal: any;

  ngOnInit() {
    this.cargarLogs();
  }

  cargarLogs() {
    this.isLoading = true;
    this.auditoriaService.filtrarLogs(this.filtro, this.currentPage, this.pageSize).subscribe({
      next: (data) => {
        this.page = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error cargando auditoría', err);
        this.isLoading = false;
      }
    });
  }

  buscar() {
    this.currentPage = 0;
    this.cargarLogs();
  }

  limpiarFiltros() {
    this.filtro = {
      accion: '',
      entidad: '',
      fechaDesde: null,
      fechaHasta: null
    };
    this.buscar();
  }

  toggleFiltros() {
    this.mostrarFiltros = !this.mostrarFiltros;
  }

  // --- MÉTODOS DEL MODAL Y TABLA (Igual que antes) ---
  verDetalles(log: Auditoria) {
    this.selectedLog = log;
    
    const rawAnteriores = log.valorAnterior ? JSON.parse(log.valorAnterior) : {};
    const rawNuevos = log.valorNuevo ? JSON.parse(log.valorNuevo) : {};
    const allKeys = new Set([...Object.keys(rawAnteriores), ...Object.keys(rawNuevos)]);

    this.datosAnteriores = [];
    this.datosNuevos = [];

    allKeys.forEach(key => {
       if (['passwordHash', 'hibernateLazyInitializer', 'handler', 'roles', 'permisos', 'password'].includes(key)) return;

       const rawValAntes = rawAnteriores[key];
       const rawValNuevo = rawNuevos[key];
       const valAntesFmt = rawAnteriores.hasOwnProperty(key) ? this.formatValue(rawValAntes) : null;
       const valNuevoFmt = rawNuevos.hasOwnProperty(key) ? this.formatValue(rawValNuevo) : null;

       this.datosAnteriores.push({
           key: this.formatKey(key),
           value: valAntesFmt, 
           rawValue: rawValAntes
       });

       this.datosNuevos.push({
           key: this.formatKey(key),
           value: valNuevoFmt,
           rawValue: rawValNuevo
       });
    });
    
    const modalEl = document.getElementById('modalDetalleAuditoria');
    if (modalEl) {
      this.detalleModal = new bootstrap.Modal(modalEl);
      this.detalleModal.show();
    }
  }
  
  esModificado(index: number): boolean {
      if (!this.datosAnteriores[index] || !this.datosNuevos[index]) return true;
      const valAntes = this.datosAnteriores[index].rawValue;
      const valNuevo = this.datosNuevos[index].rawValue;
      return JSON.stringify(valAntes) !== JSON.stringify(valNuevo);
  }

  cerrarModal() {
    if (this.detalleModal) this.detalleModal.hide();
    this.selectedLog = null;
  }

  private formatValue(val: any): string {
    if (val === null || val === undefined) return '-';
    if (typeof val === 'boolean') return val ? 'Sí' : 'No';
    if (Array.isArray(val)) {
        if (val.length === 0) return 'Ninguno';
        return val.map(item => this.formatValue(item)).join(', ');
    }
    if (typeof val === 'object') {
        if (val.nombre && val.apellido) return `${val.nombre} ${val.apellido}`;
        if (val.razonSocial) return val.razonSocial;
        if (val.nombre) return val.nombre;
        if (val.codigo) return val.codigo;
        if (val.id) return `#${val.id}`;
        return JSON.stringify(val);
    }
    return String(val);
  }

  private formatKey(key: string): string {
      const result = key.replace(/([A-Z])/g, " $1");
      return result.charAt(0).toUpperCase() + result.slice(1);
  }

  paginaAnterior() {
    if (this.page && !this.page.first) {
      this.currentPage--;
      this.cargarLogs();
    }
  }

  paginaSiguiente() {
    if (this.page && !this.page.last) {
      this.currentPage++;
      this.cargarLogs();
    }
  }

  getBadgeClass(accion: string): string {
    switch (accion) {
      case 'CREAR': return 'bg-success'; 
      case 'ACTUALIZAR': return 'bg-warning text-dark'; 
      case 'ELIMINAR': return 'bg-danger'; 
      default: return 'bg-secondary';
    }
  }
}