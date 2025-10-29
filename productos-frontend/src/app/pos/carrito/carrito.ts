import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

// Imports de Servicios
import { CarritoService } from '../../service/carrito.service';
import { VentaService } from '../../service/venta.service';
import { UsuarioService } from '../../service/usuario.service';
import { RolService } from '../../service/rol.service'; // <-- ¡IMPORTAR ROL SERVICE!

// Imports de Modelos
import { CarritoDTO } from '../../models/carrito.model';
import { ItemCarritoDTO } from '../../models/item-carrito.model';
import { VentaDTO } from '../../models/venta.model';
import { UsuarioDTO } from '../../models/usuario.model';
import { UpdateCantidadCarritoDTO } from '../../models/update-cantidad-carrito.model';
import { UsuarioFiltroDTO } from '../../models/usuario-filtro.model';

// Imports para ng-select y RxJS
import { NgSelectModule } from '@ng-select/ng-select';
import { Observable, Subject, of } from 'rxjs';
// --- ¡NUEVO IMPORT 'take'! ---
import { catchError, debounceTime, distinctUntilChanged, map, switchMap, tap, take } from 'rxjs/operators';
// --------------------------

// Utils
import { mostrarToast } from '../../utils/toast';

@Component({
  selector: 'app-carrito',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NgSelectModule],
  templateUrl: './carrito.html', // Asumo que renombraste el .html
  styleUrls: ['./carrito.css']
})
export default class CarritoComponent implements OnInit {

  // Inyección de dependencias
  private carritoService = inject(CarritoService);
  private ventaService = inject(VentaService);
  private usuarioService = inject(UsuarioService);
  private rolService = inject(RolService); // <-- ¡INYECTAR ROL SERVICE!
  private router = inject(Router);
  private fb = inject(FormBuilder);

  // Estado del componente
  public carrito: CarritoDTO | null = null;
  public isLoading = false;
  public isFinalizando = false;
  public errorMessage: string | null = null;

  // Formulario cliente
  public clienteForm: FormGroup;

  // Buscador clientes
  public clientes$: Observable<UsuarioDTO[]> = of([]);
  public clienteSearch$ = new Subject<string>();
  public isLoadingClientes = false;

  constructor() {
    this.clienteForm = this.fb.group({
      clienteId: [null, Validators.required]
    });
  }

  ngOnInit(): void {
    this.cargarCarrito();
    this.initClienteSearch();
  }

  /** Carga el carrito */
  cargarCarrito(): void {
    // ... (sin cambios aquí, tu código es correcto)
    this.isLoading = true;
    this.errorMessage = null;
    this.carritoService.getCarrito().subscribe({
      next: (data) => {
        this.carrito = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error("Error al cargar carrito:", err);
        this.errorMessage = "No se pudo cargar el carrito.";
        if (this.errorMessage) {
          mostrarToast(this.errorMessage, 'danger');
        }
        this.isLoading = false;
      }
    });
  }

  /**
   * --- ¡MÉTODO MODIFICADO PARA USAR ROL ID DINÁMICO! ---
   * Configura el Observable para buscar clientes usando el ID de Rol obtenido de la API.
   */
  initClienteSearch(): void {
    this.clientes$ = this.clienteSearch$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      // 1. Usamos switchMap para obtener PRIMERO el ID del rol cliente
      switchMap(term =>
        this.rolService.getClienteRoleId().pipe( // Llama al método cacheado del RolService
          take(1), // Solo necesitamos el ID una vez por cada búsqueda de término
          // 2. Pasamos el término y el ID obtenido (o null) al siguiente paso
          map(clienteId => ({ term: term, clienteId: clienteId }))
        )
      ),
      tap(() => this.isLoadingClientes = true), // Muestra spinner
      // 3. Usamos otro switchMap para HACER LA BÚSQUEDA de usuarios
      switchMap(({ term, clienteId }) => { // Recibimos el término y el clienteId
        // Validaciones: no buscar si no hay término, o si no pudimos obtener el rolId
        if (!term || term.length < 2 || clienteId === null) {
             // Si clienteId es null, significa que hubo un error crítico al obtenerlo.
             // Ya se mostró un toast en RolService, aquí simplemente no buscamos.
             console.warn("No se buscarán clientes porque el término es corto o el rolId es nulo:", {term, clienteId});
             return of([]);
         }

        // 4. Construimos el filtro usando el rolId DINÁMICO
        const filtro: UsuarioFiltroDTO = {
            nombreOEmail: term,
            rolId: clienteId, // <-- ¡YA NO ESTÁ HARDCODEADO!
            estado: 'ACTIVO'
        };

        // 5. Llamamos al servicio de usuarios
        return this.usuarioService.filtrarUsuarios(filtro, 0, 20).pipe(
          map(page => page.content),
          catchError(() => {
            const errorMsg = 'Error al buscar clientes';
            mostrarToast(errorMsg, 'danger');
            return of([]);
          })
        );
      }),
      tap(() => this.isLoadingClientes = false) // Oculta spinner
    );
  }

  /** Actualizar cantidad */
  actualizarCantidad(item: ItemCarritoDTO, event: Event): void {
    // ... (sin cambios aquí, tu código es correcto)
    const inputElement = event.target as HTMLInputElement;
    let nuevaCantidad = parseInt(inputElement.value, 10);

    if (isNaN(nuevaCantidad) || nuevaCantidad < 0) {
      nuevaCantidad = item.cantidad;
      inputElement.value = nuevaCantidad.toString();
      mostrarToast('Cantidad inválida', 'warning');
      return;
    }

    if (nuevaCantidad === item.cantidad) {
      return;
    }

    this.isLoading = true;
    const updateDTO: UpdateCantidadCarritoDTO = { nuevaCantidad };

    this.carritoService.actualizarCantidad(item.id!, updateDTO).subscribe({
      next: (carritoActualizado) => {
        this.carrito = carritoActualizado;
        this.isLoading = false;
      },
      error: (err) => {
        console.error("Error al actualizar cantidad:", err);
        this.errorMessage = err.error?.message || "No se pudo actualizar la cantidad (¿Stock insuficiente?).";
        if (this.errorMessage) {
            mostrarToast(this.errorMessage, 'danger');
        }
        inputElement.value = item.cantidad.toString();
        this.isLoading = false;
      }
    });
  }

  /** Quitar item */
  quitarItem(item: ItemCarritoDTO): void {
    // ... (sin cambios aquí, tu código es correcto)
     if (!confirm(`¿Seguro que deseas quitar "${item.productoNombre}" del carrito?`)) {
      return;
    }

    this.isLoading = true;
    this.carritoService.quitarItem(item.id!).subscribe({
      next: (carritoActualizado) => {
        this.carrito = carritoActualizado;
        mostrarToast(`"${item.productoNombre}" quitado del carrito`, 'success');
        this.isLoading = false;
      },
      error: (err) => {
        console.error("Error al quitar item:", err);
        this.errorMessage = "No se pudo quitar el item.";
       if (this.errorMessage) {
           mostrarToast(this.errorMessage, 'danger');
        }
        this.isLoading = false;
      }
    });
  }

  /** Vaciar carrito */
  vaciarCarrito(): void {
    // ... (sin cambios aquí, tu código es correcto)
    if (!this.carrito || !this.carrito.items || this.carrito.items.length === 0) {
      return;
    }
    if (!confirm("¿Seguro que deseas vaciar todo el carrito?")) {
      return;
    }

    this.isLoading = true;
    this.carritoService.vaciarCarrito().subscribe({
      next: (carritoVacio) => {
        this.carrito = carritoVacio;
        mostrarToast('Carrito vaciado', 'success');
        this.isLoading = false;
      },
      error: (err) => {
        console.error("Error al vaciar carrito:", err);
        this.errorMessage = "No se pudo vaciar el carrito.";
        if (this.errorMessage) {
           mostrarToast(this.errorMessage, 'danger');
        }
        this.isLoading = false;
      }
    });
  }

  /** Finalizar venta */
  finalizarVenta(): void {
    // ... (sin cambios aquí, tu código es correcto)
     this.clienteForm.markAllAsTouched();

    if (!this.carrito || !this.carrito.items || this.carrito.items.length === 0) {
      mostrarToast('El carrito está vacío.', 'warning');
      return;
    }
    if (this.clienteForm.invalid) {
      mostrarToast('Debe seleccionar un cliente para finalizar la venta.', 'warning');
      return;
    }

    if (!confirm("¿Confirmar y finalizar la venta? Esta acción descontará el stock.")) {
      return;
    }

    this.isFinalizando = true;
    this.errorMessage = null;

    const ventaDTO: VentaDTO = {
      clienteId: this.clienteForm.get('clienteId')?.value,
      detalles: this.carrito.items.map(item => ({
        productoId: item.productoId,
        cantidad: item.cantidad
      }))
    };

    this.ventaService.crearVenta(ventaDTO).subscribe({
      next: (ventaCreada) => {
        mostrarToast(`Venta #${ventaCreada.id} creada exitosamente.`, 'success');
        this.carrito = null;
        this.clienteForm.reset();
        this.isFinalizando = false;
        // this.router.navigate(['/ventas', ventaCreada.id]); // Ejemplo
        this.cargarCarrito();
      },
      error: (err) => {
        console.error("Error al finalizar la venta:", err);
        this.errorMessage = err.error?.message || "Error al procesar la venta. Verifique el stock.";
        if (this.errorMessage) {
           mostrarToast(this.errorMessage, 'danger');
        }
        this.isFinalizando = false;
        this.cargarCarrito();
      }
    });
  }
} // Fin clase CarritoComponent