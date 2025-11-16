import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms'; 

// --- Servicios y Modelos ---
import { VentaService } from '../../service/venta.service';
import { UsuarioService } from '../../service/usuario.service';
import { RolService } from '../../service/rol.service'; 
// import { AuthService } from '../../service/auth.service'; // Mentor: ELIMINADO
import { Page } from '../../models/page.model';
import { VentaDTO, EstadoVenta } from '../../models/venta.model';
import { VentaFiltroDTO } from '../../models/venta-filtro.model';
import { UsuarioDTO } from '../../models/usuario.model';
import { UsuarioFiltroDTO } from '../../models/usuario-filtro.model';

// --- RxJS y ng-select ---
import { NgSelectModule } from '@ng-select/ng-select';
import { forkJoin, Observable, Subject, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, filter, map, switchMap, tap, take } from 'rxjs/operators';

// --- Utils ---
import { mostrarToast } from '../../utils/toast';
import { HttpErrorResponse } from '@angular/common/http';

// --- Mentor: INICIO DE LA MODIFICACIÓN ---
// 1. Importar la nueva directiva de permisos
import { HasPermissionDirective } from '../../directives/has-permission.directive'; 
// --- Mentor: FIN DE LA MODIFICACIÓN ---

@Component({
  selector: 'app-ventas-list',
  standalone: true,
  imports: [
    CommonModule, 
    RouterLink, 
    ReactiveFormsModule, 
    NgSelectModule,
    // --- Mentor: INICIO DE LA MODIFICACIÓN ---
    // 2. Añadir la directiva a los imports del componente
    HasPermissionDirective
    // --- Mentor: FIN DE LA MODIFICACIÓN ---
  ],
  templateUrl: './ventas-list.html',
  styleUrls: ['./ventas-list.css']
})
export default class VentasListComponent implements OnInit {

  // --- Inyección de Dependencias ---
  private ventaService = inject(VentaService);
  private usuarioService = inject(UsuarioService);
  private rolService = inject(RolService);
  // private authService = inject(AuthService); // Mentor: ELIMINADO
  private fb = inject(FormBuilder);

  // --- Estado del Componente ---
  public ventasPage: Page<VentaDTO> | null = null;
  public isLoading = false;
  public isLoadingFilters = false;
  public errorMessage: string | null = null;
  public cancelingVentaId: number | null = null;
  
  // --- Mentor: INICIO DE LA MODIFICACIÓN ---
  // 3. 'isAdmin' se elimina por completo
  // public isAdmin: boolean = false; 
  // --- Mentor: FIN DE LA MODIFICACIÓN ---

  // --- Formulario de Filtros ---
  public filtroForm: FormGroup;
  public vendedores: UsuarioDTO[] = [];
  public estadosVenta: EstadoVenta[] = ['COMPLETADA', 'CANCELADA'];

  // --- Lógica Buscador Clientes ---
  public clientes$: Observable<UsuarioDTO[]> = of([]);
  public clienteSearch$ = new Subject<string>();
  public isLoadingClientes = false;

  // --- IDs de Roles ---
  private clienteRoleId: number | null = null;
  private vendedorRoleId: number | null = null;

  // --- Paginación ---
  public currentPage = 0;
  public pageSize = 15;

  constructor() {
    this.filtroForm = this.fb.group({
      clienteId: [{ value: null, disabled: true }], 
      vendedorId: [{ value: null, disabled: true }], 
      fechaDesde: [null],
      fechaHasta: [null],
      estado: [null]
    });
  }

  ngOnInit(): void {
    // --- Mentor: INICIO DE LA MODIFICACIÓN ---
    // 4. Esta línea se elimina
    // this.isAdmin = this.authService.hasRole('ROLE_ADMIN'); 
    // --- Mentor: FIN DE LA MODIFICACIÓN ---

    this.obtenerIdsRolesYCargarVendedores();
    this.initClienteSearch();
  }

  /** Obtiene IDs de Rol, carga Vendedores y luego la tabla */
  obtenerIdsRolesYCargarVendedores(): void {
    this.isLoadingFilters = true;
    this.filtroForm.get('clienteId')?.disable(); 
    this.filtroForm.get('vendedorId')?.disable(); // Siempre deshabilitado al inicio
 
    forkJoin({
      clienteId: this.rolService.getClienteRoleId(),
      vendedorId: this.rolService.getVendedorRoleId()
    }).subscribe({
      next: (ids) => {
        this.clienteRoleId = ids.clienteId;
        this.vendedorRoleId = ids.vendedorId;

        if (this.clienteRoleId === null || this.vendedorRoleId === null) {
          this.handleCriticalError("Error crítico al configurar roles para filtros.");
          return;
        }

        this.filtroForm.get('clienteId')?.enable();
        
        this.aplicarFiltros(true);

        // Esta lógica es correcta, el filtro de vendedor se habilita 
        // solo si se cargan los vendedores.
        this.cargarListaVendedores();
        
      },
      error: (err) => {
        console.error("Error obteniendo IDs de roles:", err);
        this.handleCriticalError("Error al configurar filtros de rol.");
      }
    });
  }

  /** Carga solo la lista de Vendedores (usando el ID obtenido) */
  cargarListaVendedores(): void {
    // Si no tenemos el ID del rol Vendedor, no podemos cargar la lista.
    if (this.vendedorRoleId === null) {
      this.isLoadingFilters = false;
      return;
    }

    const emptyUserPage: Page<UsuarioDTO> = { content: [], totalPages: 0, totalElements: 0, size: 0, number: 0 };
    const filtroVendedor: UsuarioFiltroDTO = { rolId: this.vendedorRoleId, estado: 'ACTIVO' };

    this.usuarioService.filtrarUsuarios(filtroVendedor, 0, 1000).pipe(
      catchError(() => {
        mostrarToast('Error al cargar lista de vendedores', 'danger');
        return of(emptyUserPage);
      })
    ).subscribe(page => {
        this.vendedores = page.content;
        // Habilitamos el dropdown (la directiva *appHasPermission lo mostrará u ocultará)
        this.filtroForm.get('vendedorId')?.enable(); 
        this.isLoadingFilters = false;
    });
  }

  /** Configura el Observable para buscar Clientes dinámicamente */
  initClienteSearch(): void {
     this.clientes$ = this.clienteSearch$.pipe(
      debounceTime(350),
      distinctUntilChanged(),
      filter((): boolean => this.clienteRoleId !== null),
      tap(() => this.isLoadingClientes = true),
      switchMap(term => {
         if (!term || term.length < 2) {
             return of([]);
         }
        const filtro: UsuarioFiltroDTO = {
            nombreOEmail: term,
            rolId: this.clienteRoleId!,
            estado: 'ACTIVO'
        };
        return this.usuarioService.filtrarUsuarios(filtro, 0, 20).pipe(
          map(page => page.content),
          catchError(() => {
            mostrarToast('Error al buscar clientes', 'danger');
            return of([]);
          })
        );
      }),
      tap(() => this.isLoadingClientes = false)
    );
  }

  /** Función llamada al buscar o cambiar de página */
  aplicarFiltros(resetPage: boolean = false): void {
    if (resetPage) {
        this.currentPage = 0;
    }
    this.isLoading = true;
    this.errorMessage = null;

    const filtro: VentaFiltroDTO = this.filtroForm.getRawValue();

    this.ventaService.filtrarVentas(filtro, this.currentPage, this.pageSize).subscribe({
      next: (pageData) => {
        this.ventasPage = pageData;
        this.isLoading = false;
      },
      error: (err: HttpErrorResponse) => {
        console.error("Error al cargar ventas filtradas:", err);
        this.handleCriticalError(err.error?.message || "No se pudieron cargar las ventas.");
      }
    });
  }

  /** Limpia los filtros y recarga la lista */
  limpiarFiltros(): void {
    this.filtroForm.reset({
      clienteId: null,
      vendedorId: null, 
      fechaDesde: null,
      fechaHasta: null,
      estado: null
    });
    this.aplicarFiltros(true);
  }

  // --- Métodos de Paginación y Cancelar (sin cambios) ---
  paginaAnterior(): void {
    if (this.ventasPage && this.ventasPage.number > 0) {
      this.currentPage--;
      this.aplicarFiltros();
    }
  }

  paginaSiguiente(): void {
    if (this.ventasPage && (this.ventasPage.number + 1) < this.ventasPage.totalPages) {
      this.currentPage++;
      this.aplicarFiltros();
    }
  }

  descargarComprobante(ventaId: number | undefined, event: Event): void {
    if (!ventaId) return;
    event.stopPropagation();
    mostrarToast(`Generando comprobante #${ventaId}...`, 'warning');

    this.ventaService.getComprobantePdf(ventaId).subscribe({
      next: (pdfBlob: Blob) => {
        this.downloadBlob(pdfBlob, `Comprobante-Venta-${ventaId}.pdf`);
        mostrarToast('Descarga iniciada.', 'success');
      },
      error: (err: HttpErrorResponse) => {
        this.handleCriticalError(err.error?.message || 'No se pudo descargar el comprobante.'); 
      }
    });
  }

  private downloadBlob(blob: Blob, fileName: string) {
    try {
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(link.href);
    } catch (error) {
      console.error("Error al descargar el Blob del PDF:", error);
      mostrarToast("No se pudo descargar el comprobante.", "warning");
    }
  }

  onCancelarVenta(venta: VentaDTO): void {
    if (!venta || venta.estado !== 'COMPLETADA' || !venta.id) {
      mostrarToast("Solo se pueden cancelar ventas completadas.", 'warning');
      return;
    }
    if (!confirm(`¿Seguro que deseas cancelar la Venta #${venta.id}? Esta acción repondrá el stock.`)) {
      return;
    }
    this.cancelingVentaId = venta.id;
    this.errorMessage = null;

    this.ventaService.cancelarVenta(venta.id).subscribe({
      next: () => {
        mostrarToast(`Venta #${venta.id} cancelada exitosamente. Stock repuesto.`, 'success');
        this.cancelingVentaId = null;
        this.aplicarFiltros(); 
      },
      error: (err: HttpErrorResponse) => {
        console.error("Error al cancelar venta:", err);
        this.handleCriticalError(err.error?.message || "No se pudo cancelar la venta.");
        this.cancelingVentaId = null;
      }
    });
  }

  /** Helper para errores críticos */
  private handleCriticalError(message: string): void {
      this.errorMessage = message;
      if(this.errorMessage) mostrarToast(this.errorMessage, 'danger');
      this.isLoading = false;
      this.isLoadingFilters = false;
  }
}