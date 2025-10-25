import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router'; // ActivatedRoute para leer el ID de la URL
import { HttpClientModule } from '@angular/common/http';

import { ProductoService } from '../../service/producto.service'; // Ajusta ruta
import { ProductoDTO } from '../../models/producto.model';
import { CategoriaService } from '../../service/categoria.service'; // Necesitamos las categorías
import { CategoriaDTO } from '../../models/categoria.model';
import { mostrarToast } from '../../utils/toast'; // Tu utilidad de toast

@Component({
  selector: 'app-producto-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    HttpClientModule,
    RouterLink // Para el botón Cancelar
  ],
  templateUrl: './producto-form.html',
  styleUrls: ['./producto-form.css']
})
export default class ProductoFormComponent implements OnInit {

  // Inyección de dependencias
  private fb = inject(FormBuilder);
  private productoService = inject(ProductoService);
  private categoriaService = inject(CategoriaService);
  private router = inject(Router);
  private route = inject(ActivatedRoute); // Para leer parámetros de la URL

  // Estado del componente
  public productoForm: FormGroup;
  public categorias: CategoriaDTO[] = []; // Para el <select>
  public esEdicion = false;
  public productoId: number | null = null;
  public isLoading = false;
  public pageTitle = 'Nuevo Producto'; // Título dinámico
  public errorMessage: string | null = null;

  constructor() {
    // Inicializar el formulario con validadores
    this.productoForm = this.fb.group({
      id: [null], // Campo oculto para ID en edición
      codigo: ['', [Validators.required, Validators.maxLength(50)]],
      nombre: ['', [Validators.required, Validators.maxLength(255)]],
      descripcion: [''],
      precioVenta: [null, [Validators.required, Validators.min(0)]],
      precioCosto: [null, [Validators.required, Validators.min(0)]],
      imagenUrl: ['', [Validators.maxLength(255)]],
      // stockActual: [0, [Validators.required, Validators.min(0)]], // No se edita aquí
      stockMinimo: [0, [Validators.required, Validators.min(0)]],
      estado: ['ACTIVO', Validators.required], // Valor por defecto
      categoriaId: [null, Validators.required]
    });
  }

  ngOnInit(): void {
    this.cargarCategorias();

    // Leer el ID de la URL para determinar si es edición
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.esEdicion = true;
        this.productoId = +id; // Convierte string a number
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
    this.categoriaService.listarCategorias().subscribe({
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
        // Rellenamos el formulario con los datos recibidos
        this.productoForm.patchValue({
          id: producto.id,
          codigo: producto.codigo,
          nombre: producto.nombre,
          descripcion: producto.descripcion,
          precioVenta: producto.precioVenta,
          precioCosto: producto.precioCosto,
          imagenUrl: producto.imagenUrl,
          // No cargamos stockActual aquí, se maneja por movimientos
          stockMinimo: producto.stockMinimo,
          estado: producto.estado || 'ACTIVO', // Valor por defecto si es null
          categoriaId: producto.categoriaId
        });
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

    // Lógica para decidir si crear o actualizar
    const obs = this.esEdicion
      ? this.productoService.actualizarProducto(this.productoId!, productoData)
      : this.productoService.crearProducto(productoData);

    obs.subscribe({
      next: (productoGuardado) => {
        mostrarToast(`Producto ${this.esEdicion ? 'actualizado' : 'creado'} correctamente`, 'success');
        this.isLoading = false;
        // Redirigir a la lista de productos
        this.router.navigate(['/productos']);
      },
      error: (err: any) => {
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
