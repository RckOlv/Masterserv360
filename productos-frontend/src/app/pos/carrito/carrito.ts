import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

// Imports de Servicios
import { CarritoService } from '../../service/carrito.service';
import { VentaService } from '../../service/venta.service';
import { UsuarioService } from '../../service/usuario.service';
import { RolService } from '../../service/rol.service';

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
import { catchError, debounceTime, distinctUntilChanged, map, switchMap, tap, take } from 'rxjs/operators';

// Utils
import { confirmarAccion, mostrarToast } from '../../utils/toast';

@Component({
  selector: 'app-carrito',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NgSelectModule],
  templateUrl: './carrito.html', 
  styleUrls: ['./carrito.css']
})
export default class CarritoComponent implements OnInit {

  // Inyección de dependencias
  private carritoService = inject(CarritoService);
  private ventaService = inject(VentaService);
  private usuarioService = inject(UsuarioService);
  private rolService = inject(RolService); 
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

  /** Configura el Observable para buscar clientes */
  initClienteSearch(): void {
    this.clientes$ = this.clienteSearch$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(term =>
        this.rolService.getClienteRoleId().pipe( 
          take(1), 
          map(clienteId => ({ term: term, clienteId: clienteId }))
        )
      ),
      tap(() => this.isLoadingClientes = true), 
      switchMap(({ term, clienteId }) => { 
        if (!term || term.length < 2 || clienteId === null) {
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

  /** Actualizar cantidad */
  actualizarCantidad(item: ItemCarritoDTO, event: Event): void {
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

  /** ✅ Quitar item CORREGIDO */
  quitarItem(item: ItemCarritoDTO): void {
    confirmarAccion(
      'Quitar Producto', 
      `¿Seguro que deseas quitar "${item.productoNombre}" del carrito?`
    ).then((confirmado) => {
      if (confirmado) {
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
    });
  }

  /** ✅ Vaciar carrito CORREGIDO */
  vaciarCarrito(): void {
    if (!this.carrito || !this.carrito.items || this.carrito.items.length === 0) {
      return;
    }

    confirmarAccion(
      'Vaciar Carrito', 
      '¿Seguro que deseas vaciar todo el carrito?'
    ).then((confirmado) => {
      if (confirmado) {
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
    });
  }

  /** ✅ Finalizar venta CORREGIDO */
  finalizarVenta(): void {
    this.clienteForm.markAllAsTouched();

    if (!this.carrito || !this.carrito.items || this.carrito.items.length === 0) {
      mostrarToast('El carrito está vacío.', 'warning');
      return;
    }
    if (this.clienteForm.invalid) {
      mostrarToast('Debe seleccionar un cliente para finalizar la venta.', 'warning');
      return;
    }

    confirmarAccion(
      'Finalizar Venta', 
      '¿Confirmar y finalizar la venta? Esta acción descontará el stock.'
    ).then((confirmado) => {
      if (confirmado) {
        this.isFinalizando = true;
        this.errorMessage = null;

        const ventaDTO: VentaDTO = {
          clienteId: this.clienteForm.get('clienteId')?.value,
          detalles: this.carrito!.items.map(item => ({
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
    });
  }
} // Fin clase CarritoComponent