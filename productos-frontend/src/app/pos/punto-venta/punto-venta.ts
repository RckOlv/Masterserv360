import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http'; // ¡Importante!

// --- Servicios ---
import { CarritoService } from '../../service/carrito.service';
import { VentaService } from '../../service/venta.service';
import { UsuarioService } from '../../service/usuario.service';
import { ProductoService } from '../../service/producto.service';
import { RolService } from '../../service/rol.service';

// --- Modelos ---
import { CarritoDTO } from '../../models/carrito.model';
import { ItemCarritoDTO } from '../../models/item-carrito.model';
import { VentaDTO } from '../../models/venta.model';
import { UsuarioDTO } from '../../models/usuario.model';
import { UpdateCantidadCarritoDTO } from '../../models/update-cantidad-carrito.model';
import { UsuarioFiltroDTO } from '../../models/usuario-filtro.model';
import { ProductoDTO } from '../../models/producto.model';
import { AddItemCarritoDTO } from '../../models/add-item-carrito.model';
import { Page } from '../../models/page.model';

// --- RxJS y ng-select ---
import { NgSelectModule } from '@ng-select/ng-select';
import { Observable, Subject, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, map, switchMap, tap, take } from 'rxjs/operators';

// --- Utils ---
import { mostrarToast } from '../../utils/toast';

@Component({
  selector: 'app-punto-venta',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NgSelectModule],
  templateUrl: './punto-venta.html',
  styleUrls: ['./punto-venta.css']
})
export default class PuntoVentaComponent implements OnInit {

  // --- Inyección de Dependencias ---
  private carritoService = inject(CarritoService);
  private ventaService = inject(VentaService);
  private usuarioService = inject(UsuarioService);
  private productoService = inject(ProductoService);
  private rolService = inject(RolService);
  private router = inject(Router);
  private fb = inject(FormBuilder);

  // --- Estado del Componente ---
  public carrito: CarritoDTO | null = null;
  public isLoadingCarrito = true;
  public isFinalizandoVenta = false;
  
  // --- ¡CORRECCIÓN 1: RESTAURAMOS ESTA PROPIEDAD! ---
  public errorMessage: string | null = null;
  // --------------------------------------------------

  // --- Buscador de Productos ---
  public productoSearchForm: FormGroup;
  public productos$: Observable<ProductoDTO[]> = of([]);
  public productoSearch$ = new Subject<string>();
  public isLoadingProductos = false;

  // --- Buscador/Selector de Clientes ---
  public clienteForm: FormGroup;
  public clientes$: Observable<UsuarioDTO[]> = of([]);
  public clienteSearch$ = new Subject<string>();
  public isLoadingClientes = false;

  constructor() {
    this.productoSearchForm = this.fb.group({
      productoSeleccionado: [null, Validators.required],
      cantidadAgregar: [1, [Validators.required, Validators.min(1)]]
    });
    this.clienteForm = this.fb.group({
      clienteId: [null, Validators.required]
    });
  }

  ngOnInit(): void {
    this.cargarCarrito();
    this.initProductoSearch();
    this.initClienteSearch();
  }

  // --- Lógica del Carrito ---
  cargarCarrito(): void {
    this.isLoadingCarrito = true;
    this.errorMessage = null; // Limpiamos errores antiguos al cargar
    this.carritoService.getCarrito().subscribe({
      next: (data) => {
        this.carrito = data;
        this.isLoadingCarrito = false;
      },
      error: (err: HttpErrorResponse) => {
        this.handleError(err, 'Cargar Carrito'); // El handler mostrará un toast
        this.errorMessage = "No se pudo cargar el carrito."; // Damos un mensaje al HTML
        this.isLoadingCarrito = false;
      }
    });
  }

  // --- Lógica Buscador Productos ---
  initProductoSearch(): void {
    this.productos$ = this.productoSearch$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      tap(() => {
        this.isLoadingProductos = true;
      }),
      switchMap(term => {
        if (!term || term.length < 2) {
          return of([]);
        }
        const filtro = { nombre: term, estado: 'ACTIVO' };
        return this.productoService.filtrarProductos(filtro, 0, 20).pipe(
          map((page: Page<ProductoDTO>): ProductoDTO[] => page.content),
          catchError(() => {
            mostrarToast('Error al buscar productos', 'danger');
            return of([]);
          })
        );
       }
      ),
      tap(() => this.isLoadingProductos = false)
    );
  }

  /** Llama al servicio para agregar el producto seleccionado al carrito */
  agregarAlCarrito(): void {
    this.productoSearchForm.markAllAsTouched();
    this.errorMessage = null; // Limpiamos error

    const productoSeleccionado = this.productoSearchForm.get('productoSeleccionado')?.value as ProductoDTO | null;
    const cantidad = this.productoSearchForm.get('cantidadAgregar')?.value;

    if (!productoSeleccionado || typeof productoSeleccionado.id !== 'number' || !cantidad || cantidad < 1) {
       mostrarToast('Seleccione un producto válido y especifique la cantidad positiva.', 'warning');
       return;
    }

    const itemToAdd: AddItemCarritoDTO = {
      productoId: productoSeleccionado.id,
      cantidad: cantidad
    };

    this.isLoadingCarrito = true;
    this.carritoService.agregarItem(itemToAdd).subscribe({
      next: (carritoActualizado) => {
        this.carrito = carritoActualizado;
        mostrarToast(`${itemToAdd.cantidad} x ${productoSeleccionado.nombre} agregado(s).`, 'success');
        this.productoSearchForm.reset({ productoSeleccionado: null, cantidadAgregar: 1 });
        this.isLoadingCarrito = false;
      },
      error: (err: HttpErrorResponse) => {
        this.handleError(err, 'Agregar Producto');
        this.isLoadingCarrito = false;
      }
    });
  }

  // --- Lógica Buscador Clientes ---
  initClienteSearch(): void {
     this.clientes$ = this.clienteSearch$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(term =>
        this.rolService.getClienteRoleId().pipe(
          take(1),
          map(clienteId => ({ term, clienteId }))
        )
      ),
      tap(() => this.isLoadingClientes = true),
      switchMap(({ term, clienteId }) => {
        if (!term || term.length < 2 || clienteId === null) {
             console.warn("No se buscarán clientes:", {term, clienteId});
             return of([]);
         }
        const filtro: UsuarioFiltroDTO = {
            nombreOEmail: term,
            rolId: clienteId,
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

  // --- Métodos para Modificar Carrito (Llaman al servicio) ---
  actualizarCantidadCarrito(item: ItemCarritoDTO, event: Event): void {
    const inputElement = event.target as HTMLInputElement;
    let nuevaCantidad = parseInt(inputElement.value, 10);

    if (isNaN(nuevaCantidad) || nuevaCantidad < 0) {
      nuevaCantidad = item.cantidad;
      inputElement.value = nuevaCantidad.toString();
      mostrarToast('Cantidad inválida', 'warning');
      return;
    }
    if (nuevaCantidad === item.cantidad) return;

    this.isLoadingCarrito = true;
    this.errorMessage = null; // Limpiamos error
    const updateDTO: UpdateCantidadCarritoDTO = { nuevaCantidad };

    this.carritoService.actualizarCantidad(item.id!, updateDTO).subscribe({
      next: (carritoActualizado) => { this.carrito = carritoActualizado; this.isLoadingCarrito = false; },
      error: (err: HttpErrorResponse) => {
        this.handleError(err, 'Actualizar Cantidad');
        inputElement.value = item.cantidad.toString(); // Revertir UI
        this.isLoadingCarrito = false;
      }
    });
  }

  quitarDelCarrito(item: ItemCarritoDTO): void {
     if (!confirm(`Quitar "${item.productoNombre}"?`)) return;

    this.isLoadingCarrito = true;
    this.errorMessage = null; // Limpiamos error
    this.carritoService.quitarItem(item.id!).subscribe({
      next: (carritoActualizado) => {
        this.carrito = carritoActualizado;
        mostrarToast(`"${item.productoNombre}" quitado.`, 'success');
        this.isLoadingCarrito = false;
      }, // --- ¡CORRECCIÓN 2: SE ELIMINÓ EL '_' DE AQUÍ! ---
      error: (err: HttpErrorResponse) => {
        this.handleError(err, 'Quitar Producto');
        this.isLoadingCarrito = false;
      }
    });
  }

  vaciarCarrito(): void {
     if (!this.carrito?.items?.length || !confirm("Vaciar todo el carrito?")) return;

    this.isLoadingCarrito = true;
    this.errorMessage = null; // Limpiamos error
    this.carritoService.vaciarCarrito().subscribe({
      next: (carritoVacio) => {
        this.carrito = carritoVacio;
        mostrarToast('Carrito vaciado.', 'success');
        this.isLoadingCarrito = false;
      },
      error: (err: HttpErrorResponse) => {
        this.handleError(err, 'Vaciar Carrito');
        this.isLoadingCarrito = false;
      }
    });
  }

  // --- Finalizar Venta ---
  finalizarVenta(): void {
    this.clienteForm.markAllAsTouched();

    if (!this.carrito?.items?.length) {
      mostrarToast('El carrito está vacío.', 'warning');
      return;
    }
    if (this.clienteForm.invalid) {
      mostrarToast('Debe seleccionar un cliente.', 'warning');
      return;
    }
    if (!confirm("Confirmar y finalizar la venta?")) return;

    this.isFinalizandoVenta = true;
    this.errorMessage = null; // Limpiamos error

    const ventaDTO: VentaDTO = {
      clienteId: this.clienteForm.get('clienteId')?.value,
      detalles: this.carrito.items.map(item => ({
        productoId: item.productoId,
        cantidad: item.cantidad
      })),
    };

    this.ventaService.crearVenta(ventaDTO).subscribe({
      next: (ventaCreada) => {
        mostrarToast(`Venta #${ventaCreada.id} creada.`, 'success');
        
        this.carritoService.vaciarCarrito().subscribe({
            next: () => {
                this.carrito = null; 
                this.clienteForm.reset();
                this.productoSearchForm.reset({ cantidadAgregar: 1 });
                this.isFinalizandoVenta = false;
                this.cargarCarrito(); 
            },
            error: (err: HttpErrorResponse) => {
                console.error("Venta Creada, pero falló al vaciar el carrito:", err);
                mostrarToast("Venta creada, pero no se pudo limpiar el carrito. Limpie manualmente.", 'warning');
                this.isFinalizandoVenta = false;
                this.cargarCarrito();
            }
        });
      },
      error: (err: HttpErrorResponse) => {
        this.handleError(err, 'Finalizar Venta');
        this.isFinalizandoVenta = false;
      }
    });
  }

  // --- ¡NUEVO MÉTODO PRIVADO PARA MANEJAR ERRORES! (Principio DRY) ---
  private handleError(err: HttpErrorResponse, context: string) {
    console.error(`Error en '${context}':`, err);
    let userMessage = 'Error desconocido';

    if (err.status === 0) {
      userMessage = 'No se pudo conectar con el servidor. Verifique su red.';
    } else if (err.status === 400 || err.status === 409) {
      userMessage = err.error?.message || `Error al ${context.toLowerCase()}.`;
    } else if (err.status === 401 || err.status === 403) {
      userMessage = 'Su sesión ha expirado. Por favor, inicie sesión de nuevo.';
      // Opcional: Redirigir al login
      // this.router.navigate(['/login']);
    } else if (err.status >= 500) {
      userMessage = err.error?.message || 'Error interno del servidor.';
    } else {
      userMessage = `Error: ${err.statusText} (Código: ${err.status})`;
    }
    
    // Mostramos el mensaje final y unificado al usuario
    mostrarToast(userMessage, 'danger');
    
    // Asignamos el mensaje al HTML también, si lo deseas
    this.errorMessage = userMessage; 
  }

} // Fin de la clase