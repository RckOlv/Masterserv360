import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormArray } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router'; 
import { ProveedorService } from '../../service/proveedor.service';
import { CategoriaService } from '../../service/categoria.service';
import { ProveedorDTO } from '../../models/proveedor.model';
import { CategoriaDTO } from '../../models/categoria.model';
import { mostrarToast } from '../../utils/toast';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin, of } from 'rxjs'; // Importamos of

@Component({
  selector: 'app-proveedor-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterLink], 
  templateUrl: './proveedor-form.html',
  styleUrls: ['./proveedor-form.css']
})
export default class ProveedorFormComponent implements OnInit {

  // Inyección de dependencias
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute); 
  private proveedorService = inject(ProveedorService);
  private categoriaService = inject(CategoriaService);
  
  // Estado
  public proveedorForm: FormGroup;
  public pageTitle = 'Crear Nuevo Proveedor';
  public esEdicion = false;
  public proveedorId: number | null = null;
  public isLoading = false;
  public errorMessage: string | null = null;

  // Datos para Checkboxes
  public categorias: CategoriaDTO[] = [];

  constructor() {
    // Inicialización del Formulario Reactivo
    this.proveedorForm = this.fb.group({
      razonSocial: ['', [Validators.required, Validators.maxLength(255)]],
      cuit: ['', [Validators.required, Validators.maxLength(20)]],
      email: ['', [Validators.email, Validators.maxLength(100)]],
      telefono: ['', [Validators.maxLength(20)]],
      direccion: ['', [Validators.maxLength(255)]],
      estado: ['ACTIVO', [Validators.required]],
      // Campo especial para los checkboxes, es un FormArray de booleanos
      categoriaIds: this.fb.array([]) 
    });
  }

  ngOnInit(): void {
    // 1. Obtener ID si estamos editando
    this.route.params.subscribe(params => {
      if (params['id']) {
        this.proveedorId = +params['id'];
        this.esEdicion = true;
        this.pageTitle = 'Editar Proveedor #' + this.proveedorId;
      }
      this.cargarDatos();
    });
  }

  // Getter para el FormArray de categorías (simplifica el HTML)
  get categoriaIdsArray(): FormArray {
    return this.proveedorForm.get('categoriaIds') as FormArray;
  }

  // --- Lógica de Carga y Edición ---

  cargarDatos(): void {
    this.isLoading = true;
    this.errorMessage = null;

    // forkJoin espera a que TODAS las llamadas asíncronas terminen
    forkJoin({
      categorias: this.categoriaService.listarCategorias('ACTIVO'),
      proveedor: this.esEdicion && this.proveedorId ? this.proveedorService.getById(this.proveedorId) : of(null)
    }).subscribe({
      next: (results) => {
        this.categorias = results.categorias;
        this.setupCategoriasCheckboxes(results.proveedor?.categoriaIds); // 2. Configura los checkboxes

        if (this.esEdicion && results.proveedor) {
          // 3. Rellena el resto del formulario
          this.proveedorForm.patchValue(results.proveedor);
          this.proveedorForm.get('estado')?.enable(); // Habilita el estado solo en edición
        } else {
          this.proveedorForm.get('estado')?.disable(); // Deshabilita el estado en modo creación
        }

        this.isLoading = false;
      },
      error: (err: HttpErrorResponse) => {
        console.error('Error cargando datos:', err);
        this.errorMessage = err.error?.message || 'Error al cargar los datos del formulario.';
        this.isLoading = false;
      }
    });
  }

  /**
   * 🔹 Crea un FormArray de booleanos para cada categoría, marcando las que ya tiene el proveedor.
   */
  setupCategoriasCheckboxes(idsSeleccionados: number[] = []): void {
    // 1. Limpiamos el FormArray
    this.categoriaIdsArray.clear();
    
    // 2. Para cada categoría disponible, creamos un FormControl
    this.categorias.forEach(cat => {
      // Si el ID de la categoría está en la lista del proveedor, es true.
      const isSelected = idsSeleccionados.includes(cat.id!);
      this.categoriaIdsArray.push(this.fb.control(isSelected));
    });
  }

  // --- Lógica de Submit ---

  onSubmit(): void {
    this.proveedorForm.markAllAsTouched();
    if (this.proveedorForm.invalid) {
      mostrarToast('Revise los campos obligatorios.', 'warning');
      return;
    }

    this.isLoading = true;
    this.errorMessage = null;

    // 1. Obtener los IDs seleccionados del FormArray
    const categoriasSeleccionadas = this.categoriaIdsArray.controls
      .map((control, i) => control.value ? this.categorias[i].id : null)
      .filter((id): id is number => id !== null); // Filtra los nulos y asegura el tipo

    // 2. Construir el DTO de envío
    const formValue = this.proveedorForm.getRawValue(); // Incluye el estado deshabilitado
    const proveedorData: ProveedorDTO = {
      ...formValue,
      id: this.proveedorId, // Solo se envía en edición
      categoriaIds: categoriasSeleccionadas, // Sobreescribimos el FormArray con la lista limpia
    };

    // 3. Llamar al servicio
    const obs = this.esEdicion
      ? this.proveedorService.actualizar(this.proveedorId!, proveedorData)
      : this.proveedorService.crear(proveedorData);

    obs.subscribe({
      next: () => {
        mostrarToast(`Proveedor ${this.esEdicion ? 'actualizado' : 'creado'} con éxito.`, 'success');
        this.router.navigate(['/pos/proveedores']); // Navegar de vuelta a la lista
      },
      error: (err: HttpErrorResponse) => {
        console.error('Error al guardar proveedor:', err);
        this.errorMessage = err.error?.message || 'Error al guardar el proveedor.';
        if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
        this.isLoading = false;
      }
    });
  }
  
  // Helper para validación
  get f() { return this.proveedorForm.controls; }
}