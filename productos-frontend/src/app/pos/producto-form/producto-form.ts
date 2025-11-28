import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router'; // <--- 1. IMPORTAR RouterLink
import { HttpClientModule, HttpErrorResponse } from '@angular/common/http';

import { ProductoService } from '../../service/producto.service'; 
import { ProductoDTO } from '../../models/producto.model';
import { CategoriaService } from '../../service/categoria.service'; 
import { CategoriaDTO } from '../../models/categoria.model';
import { mostrarToast } from '../../utils/toast'; 
import { HasPermissionDirective } from '../../directives/has-permission.directive';

@Component({
  selector: 'app-producto-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    HttpClientModule,
    HasPermissionDirective,
    RouterLink // <--- 2. AGREGAR AQUÍ PARA QUE FUNCIONE EN EL HTML
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

  // --- Estado del componente ---
  public productoForm: FormGroup;
  public categorias: CategoriaDTO[] = []; 
  public esEdicion = false;
  public productoId: number | null = null;
  public isLoading = false;
  public pageTitle = 'Nuevo Producto'; 
  public errorMessage: string | null = null;

  constructor() {
    this.productoForm = this.fb.group({
      id: [null], 
      codigo: ['', [Validators.required, Validators.maxLength(50)]],
      nombre: ['', [Validators.required, Validators.maxLength(255)]],
      descripcion: [''],
      precioVenta: [null, [Validators.required, Validators.min(0)]],
      precioCosto: [null, [Validators.required, Validators.min(0)]],
      imagenUrl: ['', [Validators.maxLength(255)]],
      stockMinimo: [0, [Validators.required, Validators.min(0)]],
      loteReposicion: [1, [Validators.required, Validators.min(1)]],
      estado: ['ACTIVO', Validators.required], 
      categoriaId: [null, Validators.required]
    });
  }

  ngOnInit(): void {
    this.cargarCategorias();

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
      }
    });
  }

  cargarCategorias(): void {
    this.categoriaService.listarCategorias().subscribe({ 
      next: (data) => this.categorias = data,
      error: (err: any) => {
        console.error("Error cargando categorías", err);
        mostrarToast("Error al cargar las categorías", "danger");
      }
    });
  }

  cargarProductoParaEditar(): void {
    if (!this.productoId) return;

    this.isLoading = true;
    this.productoService.getProductoById(this.productoId).subscribe({
      next: (producto) => {
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
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error("Error cargando producto", err);
        this.errorMessage = "No se pudo cargar el producto.";
        this.isLoading = false;
      }
    });
  }

  onSubmit(): void {
    this.productoForm.markAllAsTouched();
    if (this.productoForm.invalid) {
      mostrarToast("Por favor, revise los campos marcados.", "warning");
      return;
    }

    this.isLoading = true;
    this.errorMessage = null;
    const productoData = this.productoForm.value as ProductoDTO;

    const obs = this.esEdicion
      ? this.productoService.actualizarProducto(this.productoId!, productoData)
      : this.productoService.crearProducto(productoData);

    obs.subscribe({
      next: () => {
        mostrarToast(`Producto ${this.esEdicion ? 'actualizado' : 'creado'} correctamente`, 'success');
        this.isLoading = false;
        this.router.navigate(['/pos/productos']);
      },
      error: (err: HttpErrorResponse) => {
        console.error(`Error al guardar producto:`, err);
        this.errorMessage = err.error?.message || 'Error al guardar el producto.';
        mostrarToast(this.errorMessage!, 'danger');
        this.isLoading = false;
      }
    });
  }

  get f() { return this.productoForm.controls; }
}