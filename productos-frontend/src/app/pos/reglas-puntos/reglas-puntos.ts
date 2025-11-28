import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { distinctUntilChanged } from 'rxjs/operators'; // <-- Importado

// Modelos y Servicios
import { ReglaPuntosService } from '../../service/regla-puntos.service';
import { CategoriaService } from '../../service/categoria.service'; 
import { RecompensaService } from '../../service/recompensa.service'; 
import { ReglaPuntosDTO } from '../../models/regla-puntos.model';
import { CategoriaDTO } from '../../models/categoria.model'; 
import { RecompensaDTO } from '../../models/recompensa.model'; 
import { TipoDescuento } from '../../models/enums/tipo-descuento.enum'; 

// Utils
import { mostrarToast } from '../../utils/toast';
import { HasPermissionDirective } from '../../directives/has-permission.directive'; 

// Declarar bootstrap para el modal
declare var bootstrap: any;

@Component({
  selector: 'app-reglas-puntos',
  standalone: true,
  imports: [
    CommonModule, 
    ReactiveFormsModule,
    HasPermissionDirective
  ], 
  templateUrl: './reglas-puntos.html',
  styleUrls: ['./reglas-puntos.css']
})
export default class ReglasPuntosComponent implements OnInit {

  // --- Inyección de Dependencias ---
  private reglaPuntosService = inject(ReglaPuntosService);
  private recompensaService = inject(RecompensaService); 
  private categoriaService = inject(CategoriaService); 
  private fb = inject(FormBuilder);

  // --- Estado del Componente ---
  public reglaActiva: ReglaPuntosDTO | null = null;
  public historialReglas: ReglaPuntosDTO[] = [];
  public categorias: CategoriaDTO[] = []; 
  public isLoading = true; 
  public isSubmitting = false; // Para el form de Regla
  public isSubmittingRecompensa = false; // Para el form del Modal
  public errorMessage: string | null = null;
  public modalErrorMessage: string | null = null;
  public tipoDescuentoEnum = TipoDescuento; // Para usar en el HTML

  // --- Formularios ---
  public reglaForm: FormGroup; // Form de "Ganar Puntos"
  public recompensaForm: FormGroup; // Form del Modal "Crear Recompensa"
  
  private recompensaModal: any; // Instancia del Modal de Bootstrap
  public esEdicionRecompensa = false;
  public recompensaEditandoId: number | null = null;

  constructor() {
    // Formulario para "Ganar Puntos" (V2 - sin equivalencia)
    this.reglaForm = this.fb.group({
      descripcion: ['', [Validators.required, Validators.maxLength(100)]],
      montoGasto: [1000, [Validators.required, Validators.min(1)]],
      puntosGanados: [100, [Validators.required, Validators.min(1)]],
      equivalenciaPuntos: [1, [Validators.min(0.01)]], // (Solo informativo)
      caducidadPuntosMeses: [12, [Validators.required, Validators.min(1)]]
    });

    // Formulario para "Canjear Puntos" (el nuevo modal)
    this.recompensaForm = this.fb.group({
      descripcion: ['', [Validators.required, Validators.maxLength(100)]],
      puntosRequeridos: [null, [Validators.required, Validators.min(1)]],
      tipoDescuento: [TipoDescuento.FIJO, [Validators.required]],
      valor: [null, [Validators.required, Validators.min(1)]],
      // Se inicia deshabilitado (lógica V2 para [disabled])
      categoriaId: [{ value: null, disabled: true }] 
    });
  }

  ngOnInit(): void {
    this.cargarDatos();
    this.cargarCategorias();
    
    const modalElement = document.getElementById('recompensaModal');
    if (modalElement) {
      this.recompensaModal = new bootstrap.Modal(modalElement);
    }
    
    // Lógica V2 para manejar el [disabled] del dropdown de categorías
    this.recompensaForm.get('tipoDescuento')?.valueChanges
      .pipe(
        distinctUntilChanged() 
      )
      .subscribe(tipo => {
        const categoriaControl = this.recompensaForm.get('categoriaId');
        
        if (tipo === TipoDescuento.PORCENTAJE) {
          categoriaControl?.enable();
        } else {
          categoriaControl?.disable();
          categoriaControl?.setValue(null);
        }
      });
  }
  
  cargarCategorias(): void {
     this.categoriaService.listarCategorias('ACTIVO').subscribe({
       next: (data) => this.categorias = data,
       error: () => mostrarToast('No se pudieron cargar las categorías para el formulario de recompensas.', 'danger')
     });
  }

  /** Carga la regla activa y el historial */
  cargarDatos(): void {
    this.isLoading = true;
    this.errorMessage = null;

    this.reglaPuntosService.getReglaActiva().subscribe({
      next: (data) => {
        this.reglaActiva = data;
        if (data) {
          this.reglaForm.patchValue({
            descripcion: data.descripcion,
            montoGasto: data.montoGasto,
            puntosGanados: data.puntosGanados,
            equivalenciaPuntos: data.equivalenciaPuntos || 1, // (Mantenemos el valor informativo)
            caducidadPuntosMeses: data.caducidadPuntosMeses
          });
        }
        this.isLoading = false; 
      },
      error: (err) => {
        if (err.status !== 404) { 
          this.errorMessage = "Error al cargar la regla activa.";
          if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
        } else {
          console.warn("No se encontró regla activa (esperado si es la primera vez).");
          this.reglaActiva = null;
        }
        this.isLoading = false;
      }
    });

    this.reglaPuntosService.listarReglas().subscribe({
        next: (data) => {
          this.historialReglas = data.filter(r => r.estadoRegla !== 'ACTIVA');
        },
        error: () => mostrarToast('Error al cargar historial de reglas.', 'warning')
    });
  }

  /** Guarda los cambios de la Regla de "Ganar Puntos" */
  onSubmitRegla(): void {
    this.reglaForm.markAllAsTouched();
    if (this.reglaForm.invalid) {
      mostrarToast('Formulario inválido. Revise los campos.', 'warning');
      return;
    }

    if (!confirm("¿Está seguro de crear esta nueva regla? La regla activa anterior (si existe) será marcada como CADUCADA.")) {
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = null;
    
    const formValue = this.reglaForm.value;
    const nuevaReglaDTO: ReglaPuntosDTO = { 
      descripcion: formValue.descripcion,
      montoGasto: formValue.montoGasto,
      puntosGanados: formValue.puntosGanados,
      equivalenciaPuntos: formValue.equivalenciaPuntos, // (Enviamos el valor informativo)
      caducidadPuntosMeses: formValue.caducidadPuntosMeses,
      recompensas: this.reglaActiva?.recompensas || [] // Mantenemos las recompensas existentes
    };
    
    this.reglaPuntosService.crearNuevaReglaActiva(nuevaReglaDTO).subscribe({
      next: (reglaCreada) => {
        mostrarToast('¡Nueva regla de puntos activada exitosamente!', 'success');
        this.isSubmitting = false;
        this.reglaForm.reset({
          descripcion: '',
          montoGasto: 1000,
          puntosGanados: 100,
          equivalenciaPuntos: 1,
          caducidadPuntosMeses: 12
        });
        this.cargarDatos(); // Recargamos todo
      },
      error: (err: HttpErrorResponse) => { 
        console.error("Error al crear regla:", err);
        this.isSubmitting = false;
        
        let mensaje = "No se pudo crear la nueva regla. Verifique los datos.";

        if (err.status === 400 && err.error?.errors && Array.isArray(err.error.errors)) {
          mensaje = err.error.errors.map((e: any) => e.defaultMessage).join('; ');
        } else if (err.error?.message) {
          mensaje = err.error.message;
        }
        
        this.errorMessage = mensaje;
        if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
      }
    });
  }

  // --- INICIO DE LÓGICA CRUD RECOMPENSAS (MODAL) ---

  abrirModalNuevaRecompensa(): void {
    this.esEdicionRecompensa = false;
    this.recompensaEditandoId = null;
    this.modalErrorMessage = null;
    this.recompensaForm.reset({
      tipoDescuento: TipoDescuento.FIJO,
      categoriaId: null
    });
    this.recompensaForm.get('categoriaId')?.disable(); // Aseguramos estado inicial
    this.recompensaModal.show();
  }

  abrirModalEditarRecompensa(recompensa: RecompensaDTO): void {
    this.esEdicionRecompensa = true;
    this.recompensaEditandoId = recompensa.id!;
    this.modalErrorMessage = null;
    this.recompensaForm.patchValue({
      descripcion: recompensa.descripcion,
      puntosRequeridos: recompensa.puntosRequeridos,
      tipoDescuento: recompensa.tipoDescuento,
      valor: recompensa.valor,
      categoriaId: recompensa.categoriaId || null
    });
    
    // Aplicamos el estado enable/disable después de cargar los datos
    if (recompensa.tipoDescuento === TipoDescuento.PORCENTAJE) {
      this.recompensaForm.get('categoriaId')?.enable();
    } else {
      this.recompensaForm.get('categoriaId')?.disable();
    }
    
    this.recompensaModal.show();
  }

  cerrarModalRecompensa(): void {
    this.recompensaModal.hide();
    this.isSubmittingRecompensa = false;
  }

  guardarRecompensa(): void {
    if (this.recompensaForm.invalid) {
      this.recompensaForm.markAllAsTouched();
      mostrarToast("Formulario de recompensa inválido.", "warning");
      return;
    }
    
    if (!this.reglaActiva) {
      this.modalErrorMessage = "No se puede crear una recompensa si no hay una Regla Activa.";
      return;
    }

    this.isSubmittingRecompensa = true;
    this.modalErrorMessage = null;

    // Usamos .getRawValue() para obtener TODOS los valores, incluidos los deshabilitados
    const recompensaData = this.recompensaForm.getRawValue() as RecompensaDTO;
    recompensaData.reglaPuntosId = this.reglaActiva.id;
    
    if (recompensaData.tipoDescuento === TipoDescuento.FIJO) {
        recompensaData.categoriaId = null; 
    }

    const obs = this.esEdicionRecompensa
      ? this.recompensaService.actualizar(this.recompensaEditandoId!, recompensaData)
      : this.recompensaService.crear(recompensaData);

    obs.subscribe({
      next: () => {
        mostrarToast(`Recompensa ${this.esEdicionRecompensa ? 'actualizada' : 'creada'}`, 'success');
        this.isSubmittingRecompensa = false;
        this.cerrarModalRecompensa();
        this.cargarDatos(); 
      },
      error: (err: HttpErrorResponse) => {
        this.isSubmittingRecompensa = false;
        this.modalErrorMessage = err.error?.message || "Error al guardar la recompensa.";
        
        if (this.modalErrorMessage) {
            mostrarToast(this.modalErrorMessage, 'danger');
        }
      }
    });
  }

  eliminarRecompensa(id: number): void {
    if (!confirm("¿Seguro que deseas eliminar esta recompensa?")) return;

    this.isLoading = true;
    this.recompensaService.eliminar(id).subscribe({
      next: () => {
        mostrarToast("Recompensa eliminada.", 'success');
        this.cargarDatos(); 
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading = false;
        const errorMsg = err.error?.message || "Error al eliminar la recompensa.";
        mostrarToast(errorMsg, 'danger');
      }
    });
  }

  get fRegla() { return this.reglaForm.controls; }
  get fRec() { return this.recompensaForm.controls; }
}