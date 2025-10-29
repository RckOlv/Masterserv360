import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

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
  public errorMessage: string | null = null;

  // --- Buscador de Productos ---
  public productoSearchForm: FormGroup;
  public productos$: Observable<ProductoDTO[]> = of([]);
  public productoSearch$ = new Subject<string>();
  public isLoadingProductos = false;
  // --- ELIMINADO ---
  // private selectedProducto: ProductoDTO | null = null;
  // ---------------

  // --- Buscador/Selector de Clientes ---
  public clienteForm: FormGroup;
  public clientes$: Observable<UsuarioDTO[]> = of([]);
  public clienteSearch$ = new Subject<string>();
  public isLoadingClientes = false;

  constructor() {
    this.productoSearchForm = this.fb.group({
      // Este control ahora guardará el OBJETO ProductoDTO completo
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
    // ... (sin cambios)
     this.isLoadingCarrito = true;
    this.errorMessage = null;
    this.carritoService.getCarrito().subscribe({
      next: (data) => {
        this.carrito = data;
        this.isLoadingCarrito = false;
      },
      error: (err) => {
        console.error("Error al cargar carrito:", err);
        this.errorMessage = "No se pudo cargar el carrito.";
        if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
        this.isLoadingCarrito = false;
      }
    });
  }

  // --- Lógica Buscador Productos ---
  initProductoSearch(): void {
    // ... (sin cambios)
     this.productos$ = this.productoSearch$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      tap(() => {
        this.isLoadingProductos = true;
        // Ya no necesitamos limpiar selectedProducto
      }),
      switchMap(term => {
        if (!term || term.length < 2) {
          return of([]);
        }
        // Adapta esto a tu API de filtrado de productos
        // ¡OJO! Asegúrate que tu DTO ProductoFiltro tenga 'nombre' o ajusta aquí
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

  // --- ELIMINADO ---
  // onProductoSeleccionado(producto: ProductoDTO): void { ... }
  // ---------------

  /** Llama al servicio para agregar el producto seleccionado al carrito */
  agregarAlCarrito(): void {
    this.productoSearchForm.markAllAsTouched();

    // --- CORRECCIÓN: Leer producto directamente del FormControl ---
    const productoSeleccionado = this.productoSearchForm.get('productoSeleccionado')?.value as ProductoDTO | null;
    const cantidad = this.productoSearchForm.get('cantidadAgregar')?.value;

    // Validación más directa
    if (!productoSeleccionado || typeof productoSeleccionado.id !== 'number' || !cantidad || cantidad < 1) {
       mostrarToast('Seleccione un producto válido y especifique la cantidad positiva.', 'warning');
       console.error("Datos inválidos para agregar:", { producto: productoSeleccionado, cantidad }); // Log
       return;
    }
    // ----------------------------------------------------------------

    const itemToAdd: AddItemCarritoDTO = {
      productoId: productoSeleccionado.id, // Ahora TypeScript sabe que tiene 'id'
      cantidad: cantidad
    };

    this.isLoadingCarrito = true;
    this.carritoService.agregarItem(itemToAdd).subscribe({
      next: (carritoActualizado) => {
        this.carrito = carritoActualizado;
        mostrarToast(`${itemToAdd.cantidad} x ${productoSeleccionado.nombre} agregado(s).`, 'success'); // Usar la variable local
        this.productoSearchForm.reset({ cantidadAgregar: 1 }); // Limpiar formulario
        // Ya no necesitamos limpiar selectedProducto
        this.isLoadingCarrito = false;
      },
      error: (err) => {
        console.error("Error al agregar item:", err);
        const errorMsg = err.error?.message || "No se pudo agregar el item (¿Stock insuficiente?).";
        mostrarToast(errorMsg, 'danger');
        this.isLoadingCarrito = false;
      }
    });
  }

  // --- Lógica Buscador Clientes ---
  initClienteSearch(): void {
    // ... (sin cambios)
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

  // --- Métodos para Modificar Carrito ---
  actualizarCantidadCarrito(item: ItemCarritoDTO, event: Event): void {
    // ... (sin cambios)
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
    const updateDTO: UpdateCantidadCarritoDTO = { nuevaCantidad };

    this.carritoService.actualizarCantidad(item.id!, updateDTO).subscribe({
      next: (carritoActualizado) => { this.carrito = carritoActualizado; this.isLoadingCarrito = false; },
      error: (err) => {
        console.error("Error al actualizar cantidad:", err);
        const errorMsg = err.error?.message || "No se pudo actualizar la cantidad.";
        mostrarToast(errorMsg, 'danger');
        inputElement.value = item.cantidad.toString();
        this.isLoadingCarrito = false;
      }
    });
  }

  quitarDelCarrito(item: ItemCarritoDTO): void {
    // ... (sin cambios)
      if (!confirm(`Quitar "${item.productoNombre}"?`)) return;

    this.isLoadingCarrito = true;
    this.carritoService.quitarItem(item.id!).subscribe({
      next: (carritoActualizado) => {
        this.carrito = carritoActualizado;
        mostrarToast(`"${item.productoNombre}" quitado.`, 'success');
        this.isLoadingCarrito = false;
      },
      error: (err) => {
        console.error("Error al quitar item:", err);
        const errorMsg = "No se pudo quitar el item.";
       if (errorMsg) mostrarToast(errorMsg, 'danger');
        this.isLoadingCarrito = false;
      }
    });
  }

  vaciarCarrito(): void {
    // ... (sin cambios)
     if (!this.carrito?.items?.length || !confirm("Vaciar todo el carrito?")) return;

    this.isLoadingCarrito = true;
    this.carritoService.vaciarCarrito().subscribe({
      next: (carritoVacio) => {
        this.carrito = carritoVacio;
        mostrarToast('Carrito vaciado.', 'success');
        this.isLoadingCarrito = false;
      },
      error: (err) => {
        console.error("Error al vaciar carrito:", err);
        const errorMsg = "No se pudo vaciar el carrito.";
        if (errorMsg) mostrarToast(errorMsg, 'danger');
        this.isLoadingCarrito = false;
      }
    });
  }

  // --- Finalizar Venta ---
  finalizarVenta(): void {
    // ... (sin cambios)
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
        mostrarToast(`Venta #${ventaCreada.id} creada.`, 'success');
        this.carrito = null;
        this.clienteForm.reset();
        this.productoSearchForm.reset({ cantidadAgregar: 1 });
        this.isFinalizandoVenta = false;
        // this.router.navigate(['/ventas', ventaCreada.id]);
        this.cargarCarrito();
      },
      error: (err) => {
        console.error("Error al finalizar la venta:", err);
        this.errorMessage = err.error?.message || "Error al procesar la venta.";
        if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
        this.isFinalizandoVenta = false;
        this.cargarCarrito();
      }
    });
  }

} // Fin de la clase