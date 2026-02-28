import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { HttpClientModule, HttpErrorResponse } from '@angular/common/http';

import { ProductoService } from '../../service/producto.service'; 
import { ProductoDTO } from '../../models/producto.model';
import { CategoriaService } from '../../service/categoria.service'; 
import { CategoriaDTO } from '../../models/categoria.model';
import { mostrarToast } from '../../utils/toast'; 
import { HasPermissionDirective } from '../../directives/has-permission.directive';
import { NgSelectModule } from '@ng-select/ng-select'; 

import { merge } from 'rxjs';
import { debounceTime } from 'rxjs/operators';

declare var bootstrap: any;

@Component({
  selector: 'app-producto-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    HttpClientModule,
    HasPermissionDirective,
    RouterLink,
    NgSelectModule 
  ],
  templateUrl: './producto-form.html',
  styleUrls: ['./producto-form.css']
})
export default class ProductoFormComponent implements OnInit {

  private fb = inject(FormBuilder);
  private productoService = inject(ProductoService);
  private categoriaService = inject(CategoriaService);
  private router = inject(Router);
  private route = inject(ActivatedRoute); 
  public productoForm: FormGroup;
  public categorias: CategoriaDTO[] = []; 
  public esEdicion = false;
  public productoId: number | null = null;
  public isLoading = false;
  public pageTitle = 'Nuevo Producto'; 
  public errorMessage: string | null = null;
  private solicitudIdOrigen: number | null = null;
  public categoriaForm: FormGroup;
  public isSavingCategoria = false;
  public mostrarModalCategoria = false;
  public previewUrl: string | ArrayBuffer | null = null;

  constructor() {
    this.productoForm = this.fb.group({
      id: [null], 
      codigo: ['', [Validators.required, Validators.maxLength(50)]],
      nombre: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(255)]],
      descripcion: [''],
      precioVenta: [null, [Validators.required, Validators.min(0)]],
      precioCosto: [0, [Validators.min(0)]],
      imagenUrl: [''],
      stockMinimo: [0, [Validators.required, Validators.min(0)]],
      loteReposicion: [1, [Validators.required, Validators.min(1)]],
      estado: ['ACTIVO', Validators.required], 
      categoriaId: [null, Validators.required]
    }, { validators: this.precioVentaMayorCostoValidator });

    this.categoriaForm = this.fb.group({
        nombre: ['', [Validators.required, Validators.minLength(3)]],
        descripcion: ['']
    });
  }

  ngOnInit(): void {
    this.cargarCategorias();

    this.route.queryParams.subscribe(params => {
        if (params['solicitudId']) {
            this.solicitudIdOrigen = +params['solicitudId'];
        }
        if (params['nombreProducto']) {
            this.productoForm.patchValue({ nombre: params['nombreProducto'] });
            mostrarToast('Datos pre-cargados desde la solicitud.', 'info');
        }
    });

    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.esEdicion = true;
        this.productoId = +id; 
        this.pageTitle = 'Editar Producto';
        this.cargarProductoParaEditar();
      } else {
        this.esEdicion = false;
        this.pageTitle = 'Nuevo Producto';
        this.setupAutoCodigo();
      }
    });
  }

  precioVentaMayorCostoValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
    const costo = control.get('precioCosto')?.value;
    const venta = control.get('precioVenta')?.value;

    if (costo !== null && venta !== null && venta < costo) {
        return { ventaMenorCosto: true };
    }
    return null;
  }

  setupAutoCodigo() {
    const nombreControl = this.productoForm.get('nombre');
    const catControl = this.productoForm.get('categoriaId');

    if (nombreControl && catControl) {
        merge(
            nombreControl.valueChanges.pipe(debounceTime(500)), 
            catControl.valueChanges
        ).subscribe(() => {
            this.intentarGenerarCodigo();
        });
    }
  }

  intentarGenerarCodigo() {
    if (this.esEdicion) return;
    const nombre = this.productoForm.get('nombre')?.value;
    const catId = this.productoForm.get('categoriaId')?.value;

    if (nombre && nombre.length >= 3 && catId) {
        this.productoService.generarCodigo(catId, nombre).subscribe({
            next: (res: any) => {
                this.productoForm.get('codigo')?.setValue(res.codigo);
            },
            error: (err) => console.error('Error generando código auto:', err)
        });
    }
  }

  cargarCategorias(): void {
    this.categoriaService.listarCategorias().subscribe({ 
      next: (data) => this.categorias = data,
      error: (err: any) => {
        mostrarToast("Error al cargar las categorías", "danger");
      }
    });
  }

  cargarProductoParaEditar(): void {
    if (!this.productoId) return;
    this.isLoading = true;
    this.productoService.getProductoById(this.productoId).subscribe({
      next: (producto) => {
        
        // 1. Cargamos el formulario
        this.productoForm.patchValue({
          id: producto.id,
          codigo: producto.codigo,
          nombre: producto.nombre,
          descripcion: producto.descripcion,
          precioVenta: producto.precioVenta,
          precioCosto: producto.precioCosto,
          imagenUrl: producto.imagenUrl,
          stockMinimo: producto.stockMinimo,
          loteReposicion: producto.loteReposicion || 1,
          estado: producto.estado || 'ACTIVO', 
          categoriaId: producto.categoriaId
        });
        this.previewUrl = producto.imagenUrl || null;
        this.isLoading = false;
      },
      error: (err: any) => {
        this.errorMessage = "No se pudo cargar el producto.";
        this.isLoading = false;
      }
    });
  }

  onSubmit(): void {
    this.productoForm.markAllAsTouched();
    
    if (this.productoForm.invalid) {
      if (this.productoForm.errors?.['ventaMenorCosto']) {
          mostrarToast("El precio de venta no puede ser menor al costo.", "warning");
      } else {
          mostrarToast("Por favor, revise los campos marcados.", "warning");
      }
      return;
    }

    this.isLoading = true;
    this.errorMessage = null;
    
    const productoData: any = this.productoForm.value;
    
    if (this.solicitudIdOrigen) {
        productoData.solicitudId = this.solicitudIdOrigen;
    }

    const obs = this.esEdicion
      ? this.productoService.actualizarProducto(this.productoId!, productoData)
      : this.productoService.crearProducto(productoData);

    obs.subscribe({
      next: () => {
        const msg = this.solicitudIdOrigen 
            ? 'Producto creado y cliente añadido a lista de espera.' 
            : `Producto ${this.esEdicion ? 'actualizado' : 'creado'} correctamente`;
            
        mostrarToast(msg, 'success');
        this.isLoading = false;
        this.router.navigate(['/pos/productos']);
      },
      error: (err: HttpErrorResponse) => {
        console.error('Error al guardar producto:', err);
        this.isLoading = false;
        if (err.error && typeof err.error === 'string') {
           this.errorMessage = err.error;
        } else if (err.error && err.error.message) {
           this.errorMessage = err.error.message;
        } else {
           this.errorMessage = 'No se pudo guardar el producto. Verifique los datos.';
        }
        
        mostrarToast(this.errorMessage!, 'danger');
      }
    });
  }

  abrirModalCategoria() {
    this.categoriaForm.reset();
    this.mostrarModalCategoria = true;
  }

  cerrarModalCategoria() {
    this.mostrarModalCategoria = false;
  }

  guardarNuevaCategoria() {
    if (this.categoriaForm.invalid) {
        this.categoriaForm.markAllAsTouched();
        return;
    }
    this.isSavingCategoria = true;
    
    this.categoriaService.crear(this.categoriaForm.value).subscribe({
        next: (nuevaCat: CategoriaDTO) => {
            mostrarToast(`Categoría "${nuevaCat.nombre}" creada.`, 'success');
            this.categorias = [...this.categorias, nuevaCat];
            this.productoForm.patchValue({ categoriaId: nuevaCat.id });
            this.isSavingCategoria = false;
            this.cerrarModalCategoria();
            this.intentarGenerarCodigo();
        },
        error: (err) => {
            console.error('Error categoria:', err);
            let msg = 'Error al crear categoría.';
            if (err.error && err.error.message) msg = err.error.message;
            mostrarToast(msg, 'danger');
            this.isSavingCategoria = false;
        }
    });
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      if (file.size > 2 * 1024 * 1024) {
        mostrarToast("La imagen es muy pesada. Máximo 2MB.", "warning");
        return;
      }

      const reader = new FileReader();
      reader.onload = (e) => {
        this.previewUrl = e.target?.result || null;
        this.productoForm.patchValue({ imagenUrl: this.previewUrl });
      };
      reader.readAsDataURL(file);
    }
  }

  removerImagen() {
    this.previewUrl = null;
    this.productoForm.patchValue({ imagenUrl: '' });
  }

  get f() { return this.productoForm.controls; }
}