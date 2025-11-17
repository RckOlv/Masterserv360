import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router'; 
import { HttpClientModule } from '@angular/common/http';
import { HttpErrorResponse } from '@angular/common/http'; // Importar HttpErrorResponse

import { ProductoService } from '../../service/producto.service'; 
import { ProductoDTO } from '../../models/producto.model';
import { CategoriaService } from '../../service/categoria.service'; 
import { CategoriaDTO } from '../../models/categoria.model';
import { mostrarToast } from '../../utils/toast'; 
import { HasPermissionDirective } from '../../directives/has-permission.directive'; // Importar Directiva

@Component({
  selector: 'app-producto-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    HttpClientModule,
    RouterLink,
    HasPermissionDirective // Añadir Directiva
  ],
  templateUrl: './producto-form.html',
  styleUrls: ['./producto-form.css']
})
export default class ProductoFormComponent implements OnInit {

  // ... (Inyecciones de dependencias - sin cambios) ...
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
    // --- Mentor: INICIO DE LA MODIFICACIÓN (Añadir Lote de Reposición) ---
    // 1. Inicializar el formulario con el nuevo campo
    this.productoForm = this.fb.group({
      id: [null], 
      codigo: ['', [Validators.required, Validators.maxLength(50)]],
      nombre: ['', [Validators.required, Validators.maxLength(255)]],
      descripcion: [''],
      precioVenta: [null, [Validators.required, Validators.min(0)]],
      precioCosto: [null, [Validators.required, Validators.min(0)]],
      imagenUrl: ['', [Validators.maxLength(255)]],
      stockMinimo: [0, [Validators.required, Validators.min(0)]],
      loteReposicion: [1, [Validators.required, Validators.min(1)]], // <-- AÑADIDO
      estado: ['ACTIVO', Validators.required], 
      categoriaId: [null, Validators.required]
    });
    // --- Mentor: FIN DE LA MODIFICACIÓN ---
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

  /** Carga las categorías para el dropdown */
  cargarCategorias(): void {
    this.categoriaService.listarCategorias().subscribe({ // Asumo que 'listarCategorias' trae todas
      next: (data) => this.categorias = data,
      error: (err: any) => {
        console.error("Error cargando categorías", err);
        mostrarToast("Error al cargar las categorías para el formulario", "danger");
      }
    });
  }

  /** Carga los datos del producto si estamos editando */
  cargarProductoParaEditar(): void {
    if (!this.productoId) return;

    this.isLoading = true;
    this.productoService.getProductoById(this.productoId).subscribe({
      next: (producto) => {
        
        // --- Mentor: INICIO DE LA MODIFICACIÓN (Añadir Lote de Reposición) ---
        // 2. Rellenamos el formulario con el nuevo campo
        this.productoForm.patchValue({
          id: producto.id,
          codigo: producto.codigo,
          nombre: producto.nombre,
          descripcion: producto.descripcion,
          precioVenta: producto.precioVenta,
          precioCosto: producto.precioCosto,
          imagenUrl: producto.imagenUrl,
          stockMinimo: producto.stockMinimo,
          loteReposicion: producto.loteReposicion || 1, // <-- AÑADIDO
          estado: producto.estado || 'ACTIVO', 
          categoriaId: producto.categoriaId
        });
        // --- Mentor: FIN DE LA MODIFICACIÓN ---
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error("Error cargando producto para editar", err);
        mostrarToast("Error al cargar los datos del producto", "danger");
        this.errorMessage = "No se pudo cargar el producto.";
        this.isLoading = false;
      }
    });
  }

  /** Se llama al guardar el formulario (Crear o Actualizar) */
  onSubmit(): void {
    this.productoForm.markAllAsTouched();
    if (this.productoForm.invalid) {
      mostrarToast("Por favor, revise los campos marcados en rojo.", "warning");
      return;
    }

    this.isLoading = true;
    this.errorMessage = null;
    const productoData = this.productoForm.value as ProductoDTO;

    /* * Mentor: No necesitamos cambiar nada aquí.
     * 'productoData' ya incluye 'loteReposicion' desde el form.
     * El backend (ProductoMapper) ya está configurado para ignorar
     * 'stockActual' y aceptar 'loteReposicion'.
    */
    const obs = this.esEdicion
      ? this.productoService.actualizarProducto(this.productoId!, productoData)
      : this.productoService.crearProducto(productoData);

    obs.subscribe({
      next: (productoGuardado) => {
        mostrarToast(`Producto ${this.esEdicion ? 'actualizado' : 'creado'} correctamente`, 'success');
        this.isLoading = false;
        this.router.navigate(['/pos/productos']);
      },
      error: (err: HttpErrorResponse) => { // Tipado correcto
        console.error(`Error al ${this.esEdicion ? 'actualizar' : 'crear'} producto:`, err);
        this.errorMessage = err.error?.message || `Error al ${this.esEdicion ? 'actualizar' : 'crear'} el producto.`;
        if (this.errorMessage) {
          mostrarToast(this.errorMessage, 'danger');
        }
        this.isLoading = false;
      }
    });
  }

  // Helper para validación en template
  get f() { return this.productoForm.controls; }
}