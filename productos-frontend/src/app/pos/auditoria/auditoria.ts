import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuditoriaService } from '../../service/auditoria.service';
import { Auditoria } from '../../models/auditoria.model';
import { Page } from '../../models/page.model';
import { HasPermissionDirective } from '../../directives/has-permission.directive';

declare var bootstrap: any;

@Component({
  selector: 'app-auditoria-list',
  standalone: true,
  imports: [CommonModule, HasPermissionDirective],
  templateUrl: './auditoria.html',
  styleUrls: ['./auditoria.css']
})
export default class AuditoriaListComponent implements OnInit {
  
  private auditoriaService = inject(AuditoriaService);
  
  page: Page<Auditoria> | null = null;
  isLoading = true;
  currentPage = 0;
  pageSize = 20;

  selectedLog: Auditoria | null = null;
  datosAnteriores: any[] = [];
  datosNuevos: any[] = [];
  private detalleModal: any;

  ngOnInit() {
    this.loadLogs();
  }

  loadLogs() {
    this.isLoading = true;
    this.auditoriaService.getLogs(this.currentPage, this.pageSize).subscribe({
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

  verDetalles(log: Auditoria) {
    this.selectedLog = log;
    // Usamos la nueva lógica de formateo inteligente
    this.datosAnteriores = this.parseJsonData(log.valorAnterior);
    this.datosNuevos = this.parseJsonData(log.valorNuevo);

    const modalEl = document.getElementById('modalDetalleAuditoria');
    if (modalEl) {
      this.detalleModal = new bootstrap.Modal(modalEl);
      this.detalleModal.show();
    }
  }

  cerrarModal() {
    if (this.detalleModal) this.detalleModal.hide();
    this.selectedLog = null;
  }

  private parseJsonData(jsonStr?: string): any[] {
    if (!jsonStr) return [];
    try {
        const obj = JSON.parse(jsonStr);
        return Object.keys(obj)
            // Filtramos campos técnicos irrelevantes
            .filter(key => !['passwordHash', 'hibernateLazyInitializer', 'handler', 'roles', 'permisos', 'password'].includes(key))
            .map(key => ({
                key: this.formatKey(key),
                value: this.formatValue(obj[key]) // <--- AQUÍ ESTÁ EL CAMBIO
            }));
    } catch (e) {
        return [];
    }
  }

  // --- MENTOR: FORMATEADOR INTELIGENTE DE OBJETOS ---
  private formatValue(val: any): string {
    // 1. Nulos
    if (val === null || val === undefined) return '-';
    
    // 2. Booleanos
    if (typeof val === 'boolean') return val ? 'Sí' : 'No';

    // 3. Listas (Arrays) - Ejemplo: Lista de Categorías
    if (Array.isArray(val)) {
        if (val.length === 0) return 'Ninguno';
        // Recursividad: Formateamos cada elemento de la lista y los unimos
        return val.map(item => this.formatValue(item)).join(', ');
    }

    // 4. Objetos Complejos (Cliente, TipoDoc, Categoria)
    if (typeof val === 'object') {
        // Intentamos encontrar la propiedad más descriptiva
        if (val.nombre && val.apellido) return `${val.nombre} ${val.apellido}`; // Clientes/Usuarios
        if (val.razonSocial) return val.razonSocial; // Proveedores
        if (val.nombre) return val.nombre; // Categorías, Productos
        if (val.nombreCorto) return val.nombreCorto; // Tipo Documento (DNI)
        if (val.nombreRol) return val.nombreRol.replace('ROLE_', ''); // Roles
        if (val.codigo) return val.codigo; // Cupones
        if (val.descripcion) return val.descripcion; 
        
        // Si no encontramos nada conocido, mostramos el ID como último recurso
        if (val.id) return `#${val.id}`;
        
        return JSON.stringify(val); // Fallback por si acaso
    }

    // 5. Primitivos (String, Number)
    return String(val);
  }
  // --------------------------------------------------

  private formatKey(key: string): string {
      // Convierte camelCase a Texto Legible (ej: tipoDocumento -> Tipo Documento)
      const result = key.replace(/([A-Z])/g, " $1");
      return result.charAt(0).toUpperCase() + result.slice(1);
  }

  paginaAnterior() {
    if (this.page && !this.page.first) {
      this.currentPage--;
      this.loadLogs();
    }
  }

  paginaSiguiente() {
    if (this.page && !this.page.last) {
      this.currentPage++;
      this.loadLogs();
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