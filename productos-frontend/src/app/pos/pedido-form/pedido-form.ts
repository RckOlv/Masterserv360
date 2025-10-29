import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormArray } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { PedidoService } from '../../service/pedido.service';
import { ProveedorService } from '../../service/proveedor.service';
import { ProductoService } from '../../service/producto.service';
import { PedidoDTO } from '../../models/pedido.model';
import { DetallePedidoDTO } from '../../models/detalle-pedido.model';
import { ProveedorDTO } from '../../models/proveedor.model';
import { ProductoDTO } from '../../models/producto.model';
import { mostrarToast } from '../../utils/toast';

// --- ¡NUEVOS IMPORTS PARA RXJS Y NG-SELECT! ---
import { NgSelectModule } from '@ng-select/ng-select';
import { Observable, Subject, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, switchMap, tap, map } from 'rxjs/operators';
// ------------------------------------------------

@Component({
  selector: 'app-pedido-form',
  standalone: true,
  // --- ¡AÑADIMOS NgSelectModule! ---
  imports: [CommonModule, ReactiveFormsModule, RouterLink, NgSelectModule],
  templateUrl: './pedido-form.html',
  styleUrls: ['./pedido-form.css']
})
export default class PedidoFormComponent implements OnInit {

  // Inyección de dependencias
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private pedidoService = inject(PedidoService);
  private proveedorService = inject(ProveedorService);
  private productoService = inject(ProductoService);
  
  public pedidoForm: FormGroup;
  public proveedores: ProveedorDTO[] = [];
  
  // --- LÓGICA DEL BUSCADOR ASÍNCRONO ---
  public productos$: Observable<ProductoDTO[]> = of([]); // El Observable para ng-select
  public productoSearch$ = new Subject<string>(); // El "trigger" que dispara la búsqueda
  public isLoadingProductos = false;
  // ------------------------------------
  
  public isLoading = false;
  public errorMessage: string | null = null;
  public pageTitle = 'Crear Nuevo Pedido a Proveedor';
  
  private empleadoId = 4; // TODO: Reemplazar por ID de usuario logueado (desde AuthService)

  constructor() {
    this.pedidoForm = this.fb.group({
      proveedorId: [null, [Validators.required]],
      detalles: this.fb.array([], [Validators.required, Validators.minLength(1)])
    });
  }

  ngOnInit(): void {
    this.cargarProveedores();
    this.initProductoSearch(); // Iniciar el "escuchador" del buscador
  }

  /** Carga SÓLO Proveedores al iniciar */
  cargarProveedores(): void {
    this.isLoading = true;
    this.proveedorService.listarProveedores('ACTIVO').subscribe({
      next: (data) => {
        this.proveedores = data;
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error('Error cargando proveedores', err);
        mostrarToast('Error al cargar proveedores', 'danger');
        this.isLoading = false;
      }
    });
  }

  /**
   * Se llama al seleccionar un proveedor.
   * Limpia los detalles y activa una búsqueda inicial (vacía).
   */
  onProveedorSeleccionado(event: any): void {
    this.detalles.clear(); // Limpia el FormArray
    this.productoSearch$.next(''); // Dispara una búsqueda (traerá los primeros 20)
  }

  /**
   * --- ¡NUEVO MÉTODO: EL CORAZÓN DE RXJS! ---
   * Configura el Observable 'productos$' para que reaccione a
   * lo que el usuario escribe en el 'productoSearch$'.
   */
  initProductoSearch(): void {
    this.productos$ = this.productoSearch$.pipe(
      debounceTime(300), // 1. Espera 300ms después de que el usuario deja de teclear
      distinctUntilChanged(), // 2. Solo busca si el texto cambió
      tap(() => this.isLoadingProductos = true), // 3. Muestra el spinner de productos
      switchMap((term) => { // 4. Cancela búsquedas anteriores y hace la nueva
        const proveedorId = this.pedidoForm.get('proveedorId')?.value;
        if (!proveedorId) {
          return of([]); // Si no hay proveedor, devuelve un array vacío
        }
        
        // 5. Llama a nuestro NUEVO servicio de búsqueda
        return this.productoService.searchProductosByProveedor(proveedorId, term).pipe(
          map(page => page.content), // 6. Extrae solo el array 'content' de la Página
          catchError(() => {
            mostrarToast('Error al buscar productos', 'danger');
            return of([]); // 7. En caso de error, devuelve array vacío
          })
        );
      }),
      tap(() => this.isLoadingProductos = false) // 8. Oculta el spinner
    );
  }

  // --- MÉTODOS DEL FORM-ARRAY (MODIFICADOS) ---

  get detalles(): FormArray {
    return this.pedidoForm.get('detalles') as FormArray;
  }

  /** Crea un nuevo FormGroup para un detalle */
  crearGrupoDetalle(): FormGroup {
    return this.fb.group({
      // --- CAMBIO CLAVE ---
      // 'productoDTO' guardará el OBJETO producto completo, no solo el ID.
      // ng-select lo necesita para mostrar el nombre.
      productoDTO: [null, Validators.required], 
      cantidad: [1, [Validators.required, Validators.min(1)]],
      // Estos están deshabilitados. "The Angular Way".
      // Ya no causarán el warning porque no usaremos [disabled] en el HTML.
      precioUnitario: [{ value: 0, disabled: true }],
      subtotal: [{ value: 0, disabled: true }]
    });
  }

  /** Añade una nueva fila de detalle vacía al formulario */
  agregarDetalle(): void {
    this.detalles.push(this.crearGrupoDetalle());
  }

  /** Quita una fila de detalle por su índice */
  quitarDetalle(index: number): void {
    this.detalles.removeAt(index);
  }

  /**
   * Se llama cuando ng-select selecciona un producto.
   * El '$event' de ng-select (producto) es el objeto ProductoDTO completo.
   */
  onProductoSeleccionado(producto: ProductoDTO, index: number): void {
    const detalleFormGroup = this.detalles.at(index) as FormGroup;
    
    let costo = 0;
    if (producto) {
      // Leemos el costo del objeto producto seleccionado
      costo = producto.precioCosto || 0;
    }
    
    detalleFormGroup.get('precioUnitario')?.setValue(costo);
    this.calcularSubtotal(index);
  }

  /** Se llama cuando el usuario cambia la cantidad */
  onCantidadCambiada(index: number): void {
    this.calcularSubtotal(index);
  }

  /** Calcula el subtotal para una fila */
  private calcularSubtotal(index: number): void {
    const detalleFormGroup = this.detalles.at(index) as FormGroup;
    // Usamos getRawValue() para leer controles deshabilitados
    const costo = detalleFormGroup.getRawValue().precioUnitario || 0;
    const cantidad = detalleFormGroup.get('cantidad')?.value || 0;
    const subtotal = (costo * cantidad);
    
    detalleFormGroup.get('subtotal')?.setValue(subtotal.toFixed(2));
  }

  // --- SUBMIT (MODIFICADO) ---

  onSubmit(): void {
    this.pedidoForm.markAllAsTouched();
    
    if (this.detalles.length === 0) {
        mostrarToast("Debe agregar al menos un producto al pedido.", "warning");
        return;
    }
    
    if (this.pedidoForm.invalid) {
      mostrarToast("Por favor, complete todos los campos obligatorios.", "warning");
      return;
    }

    this.isLoading = true;
    this.errorMessage = null;

    // Usamos getRawValue() para leer todos los valores,
    // incluyendo los campos deshabilitados (aunque no los usaremos)
    const rawFormValue = this.pedidoForm.getRawValue();
    
    const pedidoDTO: PedidoDTO = {
      proveedorId: rawFormValue.proveedorId,
      usuarioId: this.empleadoId, 
      
      // --- ¡CAMBIO CRÍTICO! ---
      // El formulario tiene 'productoDTO' (el objeto),
      // pero el backend espera 'productoId' (el número).
      // Usamos .map() para transformar los datos ANTES de enviarlos.
      detalles: rawFormValue.detalles.map((detalle: any) => ({
        productoId: detalle.productoDTO.id, // Transformamos el objeto a ID
        cantidad: detalle.cantidad
        // No enviamos precioUnitario ni subtotal, ¡perfecto!
      }))
    };

    // DEBUG: Puedes descomentar esto para ver el JSON que se envía
    // console.log("Enviando Pedido DTO:", JSON.stringify(pedidoDTO, null, 2));

    this.pedidoService.crear(pedidoDTO).subscribe({
      next: (pedidoCreado) => {
        mostrarToast(`Pedido #${pedidoCreado.id} creado exitosamente en estado PENDIENTE.`, 'success');
        this.isLoading = false;
        this.router.navigate(['/pedidos']);
      },
      error: (err: any) => {
        console.error('Error al crear el pedido:', err);
        this.errorMessage = err.error?.message || 'Error al guardar el pedido.';
        if(this.errorMessage) mostrarToast(this.errorMessage, 'danger');
        this.isLoading = false;
      }
    });
  }

  // Helper para validación
  get f() { return this.pedidoForm.controls; }
}