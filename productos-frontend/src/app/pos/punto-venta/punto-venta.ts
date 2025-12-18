import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

// --- Servicios ---
import { CarritoService } from '../../service/carrito.service';
import { VentaService } from '../../service/venta.service';
import { UsuarioService } from '../../service/usuario.service';
import { ProductoService } from '../../service/producto.service';
import { RolService } from '../../service/rol.service';
import { ClienteService } from '../../service/cliente.service'; // <--- (1) Importar ClienteService

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
import { CuponDTO } from '../../models/cupon.model';
import { ClienteDTO } from '../../models/cliente.dto'; // <--- (2) Importar ClienteDTO

// --- RxJS y ng-select ---
import { NgSelectModule } from '@ng-select/ng-select';
import { Observable, Subject, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, map, switchMap, tap, take } from 'rxjs/operators';

// --- Utils ---
import { mostrarToast } from '../../utils/toast';
import Swal from 'sweetalert2';

declare var bootstrap: any;

@Component({
  selector: 'app-punto-venta',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NgSelectModule],
  templateUrl: './punto-venta.html',
  styleUrls: ['./punto-venta.css']
})
export default class PuntoVentaComponent implements OnInit {

  private carritoService = inject(CarritoService);
  public ventaService = inject(VentaService);
  private usuarioService = inject(UsuarioService);
  private productoService = inject(ProductoService);
  private rolService = inject(RolService);
  private clienteService = inject(ClienteService); // <--- (3) Inyectar ClienteService
  private router = inject(Router);
  private fb = inject(FormBuilder);

  public carrito: CarritoDTO | null = null;
  public isLoadingCarrito = true;
  public isFinalizandoVenta = false;
  public errorMessage: string | null = null;

  public cuponAplicado: CuponDTO | null = null;
  public montoDescuento: number = 0;
  public totalFinal: number = 0;
  public isValidandoCupon = false;

  public nuevoClienteForm: FormGroup;
  public isGuardandoCliente = false;
  private modalClienteInstance: any;

  public productoSearchForm: FormGroup;
  public productos$: Observable<ProductoDTO[]> = of([]);
  public productoSearch$ = new Subject<string>();
  public isLoadingProductos = false;

  public clienteForm: FormGroup;
  public clientes$: Observable<UsuarioDTO[]> = of([]);
  public clienteSearch$ = new Subject<string>();
  public isLoadingClientes = false;

  public maxDocumentoLength: number = 8; 

  public paises = [
    { nombre: 'Argentina', codigo: '+54', bandera: '🇦🇷' },
    { nombre: 'Brasil', codigo: '+55', bandera: '🇧🇷' },
    { nombre: 'Paraguay', codigo: '+595', bandera: '🇵🇾' },
    { nombre: 'Uruguay', codigo: '+598', bandera: '🇺🇾' },
    { nombre: 'Chile', codigo: '+56', bandera: '🇨🇱' },
    { nombre: 'Bolivia', codigo: '+591', bandera: '🇧🇴' }
  ];

  constructor() {
    const textPattern = /^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/;

    this.productoSearchForm = this.fb.group({
      productoSeleccionado: [null, Validators.required],
      cantidadAgregar: [1, [Validators.required, Validators.min(1)]]
    });

    this.clienteForm = this.fb.group({
      clienteId: [null, Validators.required],
      codigoCupon: [null]
    });

    this.nuevoClienteForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.pattern(textPattern)]],
      apellido: ['', [Validators.required, Validators.pattern(textPattern)]],
      tipoDocumentoBusqueda: ['DNI', Validators.required], 
      documento: ['', [Validators.required, Validators.pattern(/^[0-9]+$/), Validators.minLength(7), Validators.maxLength(8)]],
      email: ['', [Validators.required, Validators.email]],
      codigoPais: ['+54', Validators.required], 
      telefono: ['', [Validators.required, Validators.pattern(/^[0-9]+$/)]] 
    });
  }

  ngOnInit(): void {
    this.cargarCarrito();
    this.initProductoSearch();
    this.initClienteSearch();
    this.setupDocumentValidation();
  }

  abrirModalCliente() {
    const modalElement = document.getElementById('modalNuevoCliente');
    if (modalElement) {
      this.modalClienteInstance = new bootstrap.Modal(modalElement);
      this.modalClienteInstance.show();
    }
  }

  // --- VALIDACIONES DINÁMICAS ---
  setupDocumentValidation() {
      const tipoControl = this.nuevoClienteForm.get('tipoDocumentoBusqueda');
      const docControl = this.nuevoClienteForm.get('documento');
      
      if (!tipoControl || !docControl) return;

      tipoControl.valueChanges.subscribe(tipo => {
          docControl.clearValidators();
          docControl.setValue(''); 
          
          const validators = [Validators.required];

          switch(tipo) {
              case 'DNI':
                  this.maxDocumentoLength = 8;
                  validators.push(Validators.pattern(/^[0-9]+$/));
                  validators.push(Validators.minLength(7));
                  validators.push(Validators.maxLength(8));
                  break;
              case 'CUIT':
              case 'CUIL':
                  this.maxDocumentoLength = 11;
                  validators.push(Validators.pattern(/^[0-9]+$/));
                  validators.push(Validators.minLength(11));
                  validators.push(Validators.maxLength(11));
                  break;
              case 'PAS': 
                  this.maxDocumentoLength = 20;
                  validators.push(Validators.pattern(/^[a-zA-Z0-9]+$/)); 
                  validators.push(Validators.minLength(6));
                  validators.push(Validators.maxLength(20));
                  break;
              default:
                  this.maxDocumentoLength = 20;
          }
          
          docControl.setValidators(validators);
          docControl.updateValueAndValidity();
      });
  }

  validarInputDocumento(event: any): void {
      const tipo = this.nuevoClienteForm.get('tipoDocumentoBusqueda')?.value;
      const input = event.target;
      
      if (tipo === 'PAS') {
          input.value = input.value.replace(/[^a-zA-Z0-9]/g, '');
      } else {
          input.value = input.value.replace(/[^0-9]/g, '');
      }
      
      const controlName = input.getAttribute('formControlName');
      if (controlName) this.nuevoClienteForm.get(controlName)?.setValue(input.value);
  }

  validarInputNumerico(event: any): void {
      const input = event.target;
      input.value = input.value.replace(/[^0-9]/g, '');
      const controlName = input.getAttribute('formControlName');
      if (controlName) this.nuevoClienteForm.get(controlName)?.setValue(input.value);
  }

  validarInputTexto(event: any): void {
      const input = event.target;
      input.value = input.value.replace(/[^a-zA-ZáéíóúÁÉÍÓÚñÑ\s]/g, '');
      const controlName = input.getAttribute('formControlName');
      if (controlName) this.nuevoClienteForm.get(controlName)?.setValue(input.value);
  }

  // --- (4) AQUÍ ESTÁ EL CAMBIO CLAVE ---
  guardarNuevoCliente() {
    if (this.nuevoClienteForm.invalid) {
      this.nuevoClienteForm.markAllAsTouched();
      return;
    }

    this.isGuardandoCliente = true;
    
    const formValues = this.nuevoClienteForm.value;
    
    let telefonoFinal = '';
    let numeroLimpio = formValues.telefono ? formValues.telefono.trim() : '';
    
    if (numeroLimpio) {
        if (formValues.codigoPais === '+54' && !numeroLimpio.startsWith('9')) {
            telefonoFinal = `${formValues.codigoPais}9${numeroLimpio}`;
        } else {
            telefonoFinal = `${formValues.codigoPais}${numeroLimpio}`;
        }
    }

    // Usamos el DTO correcto
    const datosCliente: ClienteDTO = {
        nombre: formValues.nombre,
        apellido: formValues.apellido,
        tipoDocumentoBusqueda: formValues.tipoDocumentoBusqueda, 
        documento: formValues.documento,
        email: formValues.email,
        telefono: telefonoFinal
    };

    // Llamamos al NUEVO servicio (que manda el email y activa la bandera)
    this.clienteService.registrarDesdePos(datosCliente).subscribe({
      next: (nuevoUsuario: any) => { // Usamos any o UsuarioDTO
        this.isGuardandoCliente = false;
        
        if (this.modalClienteInstance) {
          this.modalClienteInstance.hide();
        }
        
        mostrarToast(`Cliente ${nuevoUsuario.nombre} registrado con éxito. Se envió email de bienvenida.`, 'success');
        
        // Asignar al select
        // Nota: ng-select necesita que el item esté en la lista [items]
        // Creamos un array temporal con ese usuario para que ng-select lo muestre
        this.clientes$ = of([nuevoUsuario]); 
        this.clienteForm.patchValue({ clienteId: nuevoUsuario.id });
        
        this.nuevoClienteForm.reset({ 
            tipoDocumentoBusqueda: 'DNI',
            codigoPais: '+54'
        });
        this.maxDocumentoLength = 8;
      },
      error: (err) => {
        this.isGuardandoCliente = false;
        const msg = err.error?.message || 'Error al registrar cliente.';
        mostrarToast(msg, 'danger');
      }
    });
  }

  // ... (Resto de métodos del carrito igual) ...
  
  cargarCarrito(): void {
    this.isLoadingCarrito = true;
    this.errorMessage = null;
    this.carritoService.getCarrito().subscribe({
      next: (data) => {
        this.carrito = { ...data };
        this.isLoadingCarrito = false;
        this.calcularTotales();
      },
      error: (err: HttpErrorResponse) => {
        this.handleError(err, 'Cargar Carrito');
        this.errorMessage = "No se pudo cargar el carrito.";
        this.isLoadingCarrito = false;
      }
    });
  }

  calcularTotales(): void {
    if (!this.carrito) return;

    const subtotal = this.carrito.totalCarrito;
    this.montoDescuento = 0;

    if (this.cuponAplicado) {
      if (this.cuponAplicado.tipoDescuento === 'FIJO') {
         this.montoDescuento = this.cuponAplicado.valor;
      } 
      else if (this.cuponAplicado.tipoDescuento === 'PORCENTAJE') {
         if (this.cuponAplicado.categoriaId) {
             let montoElegible = 0;
             this.carrito.items.forEach(item => {
                 const prodCatId = Number(item.productoCategoriaId);
                 const cupCatId = Number(this.cuponAplicado!.categoriaId);
                 if (prodCatId === cupCatId) {
                     montoElegible += item.subtotal;
                 }
             });
             this.montoDescuento = (montoElegible * this.cuponAplicado.valor) / 100;
         } else {
             this.montoDescuento = (subtotal * this.cuponAplicado.valor) / 100;
         }
      }
    }
    this.totalFinal = Math.max(0, subtotal - this.montoDescuento);
  }

  aplicarCupon(): void {
    const clienteId = this.clienteForm.get('clienteId')?.value;
    const codigo = this.clienteForm.get('codigoCupon')?.value;

    if (!clienteId) {
      mostrarToast('Primero debes seleccionar un cliente.', 'warning');
      return;
    }
    if (!codigo || codigo.trim() === '') return;

    this.isValidandoCupon = true;
    this.ventaService.validarCupon(codigo, clienteId).subscribe({
      next: (cupon: CuponDTO) => {
        this.cuponAplicado = cupon;
        this.calcularTotales();
        this.isValidandoCupon = false;
        
        let infoDesc = cupon.tipoDescuento === 'PORCENTAJE' ? `${cupon.valor}%` : `$${cupon.valor}`;
        if(cupon.categoriaNombre) infoDesc += ` en ${cupon.categoriaNombre}`;
        
        Swal.fire({
          title: '¡Cupón Aplicado!',
          text: `Descuento: ${infoDesc}`,
          icon: 'success',
          toast: true,
          position: 'top-end',
          timer: 3000,
          showConfirmButton: false
        });
      },
      error: (err: HttpErrorResponse) => {
        this.cuponAplicado = null;
        this.calcularTotales();
        this.isValidandoCupon = false;
        mostrarToast(err.error?.message || 'Cupón no válido', 'danger');
      }
    });
  }

  quitarCupon(): void {
    this.cuponAplicado = null;
    this.clienteForm.patchValue({ codigoCupon: '' });
    this.calcularTotales();
    mostrarToast('Cupón quitado', 'info');
  }

  initProductoSearch(): void {
    this.productos$ = this.productoSearch$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      tap(() => this.isLoadingProductos = true),
      switchMap(term => {
        if (!term || term.length < 2) return of([]);
        const filtro = { nombre: term, estado: 'ACTIVO' };
        return this.productoService.filtrarProductos(filtro, 0, 20).pipe(
          map((page: Page<ProductoDTO>) => page.content),
          catchError(() => {
            mostrarToast('Error al buscar productos', 'danger');
            return of([]);
          })
        );
      }),
      tap(() => this.isLoadingProductos = false)
    );
  }

  agregarAlCarrito(): void {
    this.productoSearchForm.markAllAsTouched();
    this.errorMessage = null;

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
        this.carrito = { ...carritoActualizado };
        this.calcularTotales();
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
    this.errorMessage = null;
    const updateDTO: UpdateCantidadCarritoDTO = { nuevaCantidad };

    this.carritoService.actualizarCantidad(item.id!, updateDTO).subscribe({
      next: (carritoActualizado) => {
        this.carrito = { ...carritoActualizado };
        this.calcularTotales();
        this.isLoadingCarrito = false;
      },
      error: (err: HttpErrorResponse) => {
        this.handleError(err, 'Actualizar Cantidad');
        inputElement.value = item.cantidad.toString();
        this.isLoadingCarrito = false;
      }
    });
  }

  quitarDelCarrito(item: ItemCarritoDTO): void {
     if (!confirm(`Quitar "${item.productoNombre}"?`)) return;

    this.isLoadingCarrito = true;
    this.errorMessage = null;
    this.carritoService.quitarItem(item.id!).subscribe({
      next: (carritoActualizado) => {
        this.carrito = { ...carritoActualizado };
        this.calcularTotales();
        mostrarToast(`"${item.productoNombre}" quitado.`, 'success');
        this.isLoadingCarrito = false;
      },
      error: (err: HttpErrorResponse) => {
        this.handleError(err, 'Quitar Producto');
        this.isLoadingCarrito = false;
      }
    });
  }

  vaciarCarrito(): void {
     if (!this.carrito?.items?.length || !confirm("Vaciar todo el carrito?")) return;

    this.isLoadingCarrito = true;
    this.errorMessage = null;
    this.carritoService.vaciarCarrito().subscribe({
      next: (carritoVacio) => {
        this.carrito = { ...carritoVacio };
        this.calcularTotales();
         mostrarToast('Carrito vaciado.', 'success');
        this.isLoadingCarrito = false;
        },
      error: (err: HttpErrorResponse) => {
        this.handleError(err, 'Vaciar Carrito');
        this.isLoadingCarrito = false;
      }
    });
  }

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

    Swal.fire({
      title: '¿Confirmar Venta?',
      text: `Total a cobrar: $${this.totalFinal.toFixed(2)}`,
      icon: 'question',
      showCancelButton: true,
      confirmButtonColor: '#198754',
      cancelButtonColor: '#d33',
      confirmButtonText: 'Sí, finalizar',
      cancelButtonText: 'Cancelar',
      background: '#1e1e1e',
      color: '#ffffff'
    }).then((result) => {
      if (result.isConfirmed) {
        this.procesarVenta();
      }
    });
  }

  procesarVenta() {
    this.isFinalizandoVenta = true;
    this.errorMessage = null;

    const ventaDTO: VentaDTO = {
      clienteId: this.clienteForm.get('clienteId')?.value,
      codigoCupon: this.clienteForm.get('codigoCupon')?.value || null,
      detalles: this.carrito!.items.map(item => ({
        productoId: item.productoId,
        cantidad: item.cantidad,
        productoNombre: item.productoNombre,
        productoCodigo: item.productoCodigo,
        precioUnitario: item.precioUnitarioVenta,
        subtotal: item.subtotal
      })),
      id: undefined
    };

    this.ventaService.crearVenta(ventaDTO).subscribe({
      next: (ventaCreada: VentaDTO) => {
        
       Swal.fire({
          title: '¡Venta Exitosa!',
          html: `
            <p>Comprobante <strong>#${ventaCreada.id}</strong> generado correctamente.</p>
            <p class="mb-0 text-success"><i class="bi bi-check-circle-fill"></i> Se ha enviado una copia al email del cliente.</p>
          `,
          icon: 'success',
          background: '#1e1e1e',
          color: '#ffffff',
          timer: 6000, 
          showConfirmButton: true, 
          confirmButtonColor: '#198754',
          confirmButtonText: 'Aceptar'
        });

        this.carrito = null;
        this.clienteForm.reset();
        this.productoSearchForm.reset({ cantidadAgregar: 1 });
        this.cuponAplicado = null;
        this.montoDescuento = 0;
        this.totalFinal = 0;
        this.isFinalizandoVenta = false;

        this.cargarCarrito();
      },
      error: (err: HttpErrorResponse) => {
        this.handleError(err, 'Finalizar Venta');
        this.isFinalizandoVenta = false;
      }
    });
  }

  private handleError(err: HttpErrorResponse, context: string) {
    console.error(`Error en '${context}':`, err);
    let userMessage = 'Error desconocido';

    if (err.status === 0) {
      userMessage = 'No se pudo conectar con el servidor.';
    } else if (err.status === 400 || err.status === 409) {
      userMessage = err.error?.message || `Error al ${context.toLowerCase()}.`;
    } else {
      userMessage = `Error: ${err.statusText} (Código: ${err.status})`;
    }
    
    mostrarToast(userMessage, 'danger');
    this.errorMessage = userMessage;
  }
}