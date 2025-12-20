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
import { NgSelectModule } from '@ng-select/ng-select';
import { Observable, Subject, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, switchMap, tap, map } from 'rxjs/operators';
import { AuthService } from '../../service/auth.service'; // <--- Importado

@Component({
  selector: 'app-pedido-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, NgSelectModule],
  templateUrl: './pedido-form.html',
  styleUrls: ['./pedido-form.css']
})
export default class PedidoFormComponent implements OnInit {

  private fb = inject(FormBuilder);
  private router = inject(Router);
  private pedidoService = inject(PedidoService);
  private proveedorService = inject(ProveedorService);
  private productoService = inject(ProductoService);
  private authService = inject(AuthService); // <--- Inyectado
  
  public pedidoForm: FormGroup;
  public proveedores: ProveedorDTO[] = [];
  
  public productos$: Observable<ProductoDTO[]> = of([]); 
  public productoSearch$ = new Subject<string>(); 
  public isLoadingProductos = false;
  
  // CORRECCIÓN: Inicializar como null, NO poner un número fijo
  private empleadoId: number | null = null;
  
  public isLoading = false;
  public errorMessage: string | null = null;
  public pageTitle = 'Crear Nuevo Pedido a Proveedor';
  

  constructor() {
    this.pedidoForm = this.fb.group({
      proveedorId: [null, [Validators.required]],
      detalles: this.fb.array([], [Validators.required, Validators.minLength(1)])
    });
  }

  ngOnInit(): void {
    // CORRECCIÓN: Obtener ID real del usuario desde el token
    const tokenData = this.authService.getDecodedToken();
    
    if (tokenData && tokenData.id) {
        this.empleadoId = tokenData.id;
    } else {
        mostrarToast('Error: No se pudo identificar al usuario. Reloguee por favor.', 'danger');
        this.router.navigate(['/auth/login']);
        return; // Detenemos la carga si no hay usuario
    }

    this.cargarProveedores();
    this.initProductoSearch();
  }

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

  onProveedorSeleccionado(event: any): void {
    this.detalles.clear();
    this.productoSearch$.next('');
  }

  initProductoSearch(): void {
    this.productos$ = this.productoSearch$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      tap(() => this.isLoadingProductos = true),
      switchMap((term) => { 
        const proveedorId = this.pedidoForm.get('proveedorId')?.value;
        if (!proveedorId) {
          return of([]);
        }
        
        return this.productoService.searchProductosByProveedor(proveedorId, term).pipe(
          map(page => page.content),
          catchError(() => {
            mostrarToast('Error al buscar productos', 'danger');
            return of([]);
          })
        );
      }),
      tap(() => this.isLoadingProductos = false)
    );
  }

  get detalles(): FormArray {
    return this.pedidoForm.get('detalles') as FormArray;
  }

  crearGrupoDetalle(): FormGroup {
    return this.fb.group({
      productoDTO: [null, Validators.required],
      cantidad: [1, [Validators.required, Validators.min(1), Validators.pattern('^[0-9]+$')]],
      precioUnitario: [0, [Validators.required, Validators.min(0)]],
      subtotal: [0, Validators.required]
    });
  }

  agregarDetalle(): void {
    this.detalles.push(this.crearGrupoDetalle());
    this.detalles.markAsDirty();
  }

  quitarDetalle(index: number): void {
    this.detalles.removeAt(index);
    this.detalles.markAsDirty();
  }

  onProductoSeleccionado(producto: ProductoDTO, index: number): void {

    // Validación: no permitir productos duplicados
    const existe = this.detalles.controls.some((g, idx) =>
      idx !== index && g.get('productoDTO')?.value?.id === producto?.id
    );
    if (existe) {
      mostrarToast("Este producto ya fue agregado al pedido.", "warning");
      const grp = this.detalles.at(index) as FormGroup;
      grp.get('productoDTO')?.setValue(null);
      return;
    }

    const detalleFormGroup = this.detalles.at(index) as FormGroup;
    
    let costo = producto ? (producto.precioCosto || 0) : 0;
    detalleFormGroup.get('precioUnitario')?.setValue(costo);
    
    this.calcularSubtotal(index);
  }

  onCantidadCambiada(index: number): void {
    const group = this.detalles.at(index) as FormGroup;

    // Validación cantidad > 0
    const value = group.get('cantidad')?.value;
    if (value <= 0) {
      group.get('cantidad')?.setErrors({ min: true });
    }

    this.calcularSubtotal(index);
  }

  private calcularSubtotal(index: number): void {
    const detalleFormGroup = this.detalles.at(index) as FormGroup;
    const costo = detalleFormGroup.get('precioUnitario')?.value || 0;
    const cantidad = detalleFormGroup.get('cantidad')?.value || 0;

    const subtotal = costo * cantidad;
    detalleFormGroup.get('subtotal')?.setValue(subtotal.toFixed(2));
  }

  // SUBMIT CON VALIDACIONES
  onSubmit(): void {
    this.pedidoForm.markAllAsTouched();

    // Validación: debe tener al menos un detalle
    if (this.detalles.length === 0) {
      mostrarToast("Debe agregar al menos un producto al pedido.", "warning");
      this.detalles.setErrors({ minlength: true });
      return;
    }

    // Validación: todos los grupos deben estar completos
    for (let i = 0; i < this.detalles.length; i++) {
      const g = this.detalles.at(i) as FormGroup;
      if (g.invalid) {
        mostrarToast("Hay filas con datos incompletos o inválidos.", "warning");
        return;
      }
    }

    if (this.pedidoForm.invalid) {
      mostrarToast("Por favor revise los campos marcados.", "warning");
      return;
    }

    // Validación extra de seguridad: Si no tenemos ID de usuario, abortamos
    if (!this.empleadoId) {
        mostrarToast("Error de sesión: Usuario no identificado.", "danger");
        return;
    }

    this.isLoading = true;
    this.errorMessage = null;

    const rawFormValue = this.pedidoForm.getRawValue();

    const pedidoDTO: PedidoDTO = {
      proveedorId: rawFormValue.proveedorId,
      usuarioId: this.empleadoId!, // CORRECCIÓN: Usamos el ID dinámico con !
      detalles: rawFormValue.detalles.map((detalle: any) => ({
        productoId: detalle.productoDTO.id,
        cantidad: detalle.cantidad
      }))
    };

    this.pedidoService.crear(pedidoDTO).subscribe({
      next: (pedidoCreado) => {
        mostrarToast(`Pedido #${pedidoCreado.id} creado exitosamente en estado PENDIENTE.`, 'success');
        this.isLoading = false;
        this.router.navigate(['/pos/pedidos']);
      },
      error: (err: any) => {
        console.error('Error al crear el pedido:', err);
        this.errorMessage = err.error?.message || 'Error al guardar el pedido.';
        if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
        this.isLoading = false;
      }
    });
  }

  get f() { return this.pedidoForm.controls; }
}