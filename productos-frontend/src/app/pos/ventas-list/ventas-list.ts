import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms'; // Importar Forms

// --- Servicios y Modelos ---
import { VentaService } from '../../service/venta.service';
import { UsuarioService } from '../../service/usuario.service';
import { RolService } from '../../service/rol.service'; // Importar RolService
import { AuthService } from '../../service/auth.service'; // <-- ¡IMPORTAR AUTH SERVICE!
import { Page } from '../../models/page.model';
import { VentaDTO, EstadoVenta } from '../../models/venta.model';
import { VentaFiltroDTO } from '../../models/venta-filtro.model';
import { UsuarioDTO } from '../../models/usuario.model';
import { UsuarioFiltroDTO } from '../../models/usuario-filtro.model';

// --- RxJS y ng-select ---
import { NgSelectModule } from '@ng-select/ng-select'; // <-- Importar NgSelectModule
import { forkJoin, Observable, Subject, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, filter, map, switchMap, tap, take } from 'rxjs/operators';

// --- Utils ---
import { mostrarToast } from '../../utils/toast';

@Component({
  selector: 'app-ventas-list',
  standalone: true,
  // --- ¡ASEGURAR NgSelectModule EN IMPORTS! ---
  imports: [CommonModule, RouterLink, ReactiveFormsModule, NgSelectModule],
  // --------------------------------------------
  templateUrl: './ventas-list.html',
  styleUrls: ['./ventas-list.css']
})
export default class VentasListComponent implements OnInit {

  // --- Inyección de Dependencias ---
  private ventaService = inject(VentaService);
  private usuarioService = inject(UsuarioService);
  private rolService = inject(RolService); // Inyectar RolService
  private authService = inject(AuthService); // <-- ¡INYECTAR AUTH SERVICE!
  private fb = inject(FormBuilder);

  // --- Estado del Componente ---
  public ventasPage: Page<VentaDTO> | null = null;
  public isLoading = false;
  public isLoadingFilters = false;
  public errorMessage: string | null = null;
  public cancelingVentaId: number | null = null;
  public isAdmin: boolean = false; // <-- Propiedad para controlar visibilidad

  // --- Formulario de Filtros ---
  public filtroForm: FormGroup;
  // Ya no necesitamos 'clientes: UsuarioDTO[] = [];'
  public vendedores: UsuarioDTO[] = [];
  public estadosVenta: EstadoVenta[] = ['COMPLETADA', 'CANCELADA'];

  // --- Lógica Buscador Clientes ---
  public clientes$: Observable<UsuarioDTO[]> = of([]);
  public clienteSearch$ = new Subject<string>();
  public isLoadingClientes = false;

  // --- IDs de Roles (obtenidos dinámicamente) ---
  private clienteRoleId: number | null = null;
  private vendedorRoleId: number | null = null;

  // --- Paginación ---
  public currentPage = 0;
  public pageSize = 15;

  constructor() {
    // Inicializar los FormControl, deshabilitados para evitar warnings y esperar datos
    this.filtroForm = this.fb.group({
      clienteId: [{ value: null, disabled: true }], // Deshabilitar inicialmente
      vendedorId: [{ value: null, disabled: true }], // Deshabilitar inicialmente
      fechaDesde: [null],
      fechaHasta: [null],
      estado: [null]
    });
  }

  ngOnInit(): void {
    // 1. Obtener Rol Actual
    this.isAdmin = this.authService.hasRole('ROLE_ADMIN'); // Determinar si el usuario es Admin

    // 2. Cargar datos
    this.obtenerIdsRolesYCargarVendedores();
    this.initClienteSearch();
  }

  /** Obtiene IDs de Rol, carga Vendedores y luego la tabla */
  obtenerIdsRolesYCargarVendedores(): void {
    this.isLoadingFilters = true;

    // Deshabilitar controles que dependen de esta carga
    this.filtroForm.get('clienteId')?.disable();
    this.filtroForm.get('vendedorId')?.disable();

    forkJoin({
      clienteId: this.rolService.getClienteRoleId(),
      vendedorId: this.rolService.getVendedorRoleId()
    }).subscribe({
      next: (ids) => {
        this.clienteRoleId = ids.clienteId;
        this.vendedorRoleId = ids.vendedorId;

        // Validar que obtuvimos los IDs
        if (this.clienteRoleId === null || this.vendedorRoleId === null) {
          this.handleCriticalError("Error crítico al configurar roles para filtros.");
          return;
        }

        // Cargar lista estática de Vendedores (usando el ID)
        this.cargarListaVendedores();
        // Carga inicial de la tabla
        this.aplicarFiltros(true);
      },
      error: (err) => {
        console.error("Error obteniendo IDs de roles:", err);
        this.handleCriticalError("Error al configurar filtros de rol.");
      }
    });
  }

   /** Carga solo la lista de Vendedores (usando el ID obtenido) */
  cargarListaVendedores(): void {
    if (this.vendedorRoleId === null) return;
    const emptyUserPage: Page<UsuarioDTO> = { content: [], totalPages: 0, totalElements: 0, size: 0, number: 0 };
    const filtroVendedor: UsuarioFiltroDTO = { rolId: this.vendedorRoleId, estado: 'ACTIVO' };

    this.usuarioService.filtrarUsuarios(filtroVendedor, 0, 1000).pipe(
      catchError(() => {
        mostrarToast('Error al cargar lista de vendedores', 'danger');
        return of(emptyUserPage);
      })
    ).subscribe(page => {
        this.vendedores = page.content;
        this.filtroForm.get('vendedorId')?.enable(); // HABILITAR SELECT VENDEDOR
        this.filtroForm.get('clienteId')?.enable(); // HABILITAR NG-SELECT CLIENTE
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

    const filtro: VentaFiltroDTO = this.filtroForm.value;

    this.ventaService.filtrarVentas(filtro, this.currentPage, this.pageSize).subscribe({
      next: (pageData) => {
        this.ventasPage = pageData;
        this.isLoading = false;
      },
      error: (err) => {
        console.error("Error al cargar ventas filtradas:", err);
        this.errorMessage = "No se pudieron cargar las ventas con los filtros aplicados.";
        if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
        this.isLoading = false;
      }
    });
  }

  /** Limpia los filtros y recarga la lista */
  limpiarFiltros(): void {
    this.filtroForm.reset();
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

  irAPagina(pageNumber: number): void {
     if (pageNumber >= 0 && pageNumber < (this.ventasPage?.totalPages ?? 0)) {
         this.currentPage = pageNumber;
         this.aplicarFiltros();
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
        this.aplicarFiltros(); // Recargar CON los filtros actuales
      },
      error: (err) => {
        console.error("Error al cancelar venta:", err);
        this.errorMessage = err.error?.message || "No se pudo cancelar la venta.";
        if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
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