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
import { forkJoin, of } from 'rxjs';

@Component({
  selector: 'app-proveedor-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterLink], 
  templateUrl: './proveedor-form.html',
  styleUrls: ['./proveedor-form.css']
})
export default class ProveedorFormComponent implements OnInit {

  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute); 
  private proveedorService = inject(ProveedorService);
  private categoriaService = inject(CategoriaService);
  
  public proveedorForm: FormGroup;
  public pageTitle = 'Crear Nuevo Proveedor';
  public esEdicion = false;
  public proveedorId: number | null = null;
  public isLoading = false;
  public errorMessage: string | null = null;

  public categorias: CategoriaDTO[] = [];

  // Lista de Países
  public paises = [
    { nombre: 'Argentina', codigo: '+54', bandera: '🇦🇷' },
    { nombre: 'Brasil', codigo: '+55', bandera: '🇧🇷' },
    { nombre: 'Paraguay', codigo: '+595', bandera: '🇵🇾' },
    { nombre: 'Uruguay', codigo: '+598', bandera: '🇺🇾' },
    { nombre: 'Chile', codigo: '+56', bandera: '🇨🇱' },
    { nombre: 'Bolivia', codigo: '+591', bandera: '🇧🇴' }
  ];

  constructor() {
    this.proveedorForm = this.fb.group({
      razonSocial: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
      cuit: ['', [Validators.required, Validators.pattern(/^[0-9]{11}$/)]], 
      email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
      codigoPais: ['+54'], 
      telefono: ['', [Validators.minLength(8), Validators.maxLength(15)]], 
      direccion: ['', [Validators.maxLength(255)]],
      estado: ['ACTIVO', [Validators.required]],
      categoriaIds: this.fb.array([]) 
    });
  }

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      if (params['id']) {
        this.proveedorId = +params['id'];
        this.esEdicion = true;
        this.pageTitle = 'Editar Proveedor #' + this.proveedorId;
      }
      this.cargarDatos();
    });
  }

  get categoriaIdsArray(): FormArray {
    return this.proveedorForm.get('categoriaIds') as FormArray;
  }
  
  validarInputNumerico(event: any): void {
      const input = event.target;
      input.value = input.value.replace(/[^0-9]/g, '');
      const controlName = input.getAttribute('formControlName');
      if (controlName) this.proveedorForm.get(controlName)?.setValue(input.value);
  }

  cargarDatos(): void {
    this.isLoading = true;
    this.errorMessage = null;

    forkJoin({
      categorias: this.categoriaService.listarCategorias('ACTIVO'),
      proveedor: this.esEdicion && this.proveedorId ? this.proveedorService.getById(this.proveedorId) : of(null)
    }).subscribe({
      next: (results) => {
        this.categorias = results.categorias;
        this.setupCategoriasCheckboxes(results.proveedor?.categoriaIds);

        if (this.esEdicion && results.proveedor) {
          let telefonoFull = results.proveedor.telefono || '';
          let codigo = '+54';
          let numero = telefonoFull;

          if (telefonoFull.startsWith('+549')) {
              codigo = '+54';
              numero = telefonoFull.substring(4); 
          } 
          else {
              const paisEncontrado = this.paises.find(p => telefonoFull.startsWith(p.codigo));
              if (paisEncontrado) {
                  codigo = paisEncontrado.codigo;
                  numero = telefonoFull.substring(codigo.length);
              }
          }

          this.proveedorForm.patchValue({
              ...results.proveedor,
              codigoPais: codigo,
              telefono: numero
          });
          this.proveedorForm.get('estado')?.enable();
        } else {
          this.proveedorForm.get('estado')?.disable();
        }

        this.isLoading = false;
      },
      error: (err: HttpErrorResponse) => {
        console.error('Error cargando datos:', err);
        this.errorMessage = err.error?.message || 'Error al cargar los datos.';
        this.isLoading = false;
      }
    });
  }

  setupCategoriasCheckboxes(idsSeleccionados: number[] = []): void {
    this.categoriaIdsArray.clear();
    this.categorias.forEach(cat => {
      const isSelected = idsSeleccionados ? idsSeleccionados.includes(cat.id!) : false;
      this.categoriaIdsArray.push(this.fb.control(isSelected));
    });
  }

  onSubmit(): void {
    if (this.proveedorForm.invalid) {
      this.proveedorForm.markAllAsTouched();
      mostrarToast('Revise los campos obligatorios.', 'warning');
      return;
    }

    this.isLoading = true;
    this.errorMessage = null;

    const categoriasSeleccionadas = this.categoriaIdsArray.controls
      .map((control, i) => control.value ? this.categorias[i].id : null)
      .filter((id): id is number => id !== null);

    const { codigoPais, ...datosLimpios } = this.proveedorForm.getRawValue();

    let telefonoFinal = '';
    let numeroLimpio = datosLimpios.telefono ? datosLimpios.telefono.trim() : '';

    if (numeroLimpio) {
        if (codigoPais === '+54' && !numeroLimpio.startsWith('9')) {
            telefonoFinal = `${codigoPais}9${numeroLimpio}`;
        } else {
            telefonoFinal = `${codigoPais}${numeroLimpio}`;
        }
    }

    const proveedorData: ProveedorDTO = {
      ...datosLimpios, 
      telefono: telefonoFinal,
      id: this.proveedorId,
      categoriaIds: categoriasSeleccionadas,
    };

    const obs = this.esEdicion
      ? this.proveedorService.actualizar(this.proveedorId!, proveedorData)
      : this.proveedorService.crear(proveedorData);

    obs.subscribe({
      next: () => {
        mostrarToast(`Proveedor ${this.esEdicion ? 'actualizado' : 'creado'} con éxito.`, 'success');
        this.router.navigate(['/pos/proveedores']);
      },
      // --- MENTOR: ERROR HANDLING MEJORADO ---
      error: (err: HttpErrorResponse) => {
        console.error('Error al guardar proveedor:', err);
        this.isLoading = false;

        // 1. Mensaje del Backend (Prioridad)
        if (err.error && typeof err.error === 'string') {
            this.errorMessage = err.error;
        } else if (err.error && err.error.message) {
            this.errorMessage = err.error.message;
        } else {
            // 2. Fallback por si acaso
            this.errorMessage = 'Error al guardar el proveedor.';
        }

        // 3. Chequeo extra por si el backend mandó la excepción cruda de BD
        if (this.errorMessage?.includes('violates unique constraint')) {
              this.errorMessage = 'Error: El CUIT o Razón Social ya existen.';
        }

        mostrarToast(this.errorMessage!, 'danger');
      }
      // -------------------------------------
    });
  }
  
  get f() { return this.proveedorForm.controls; }
}