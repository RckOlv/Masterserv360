import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { distinctUntilChanged } from 'rxjs/operators'; 

import { ReglaPuntosService } from '../../service/regla-puntos.service';
import { CategoriaService } from '../../service/categoria.service'; 
import { RecompensaService } from '../../service/recompensa.service'; 
import { ReglaPuntosDTO } from '../../models/regla-puntos.model';
import { CategoriaDTO } from '../../models/categoria.model'; 
import { RecompensaDTO } from '../../models/recompensa.model'; 
import { TipoDescuento } from '../../models/enums/tipo-descuento.enum'; 
import { mostrarToast } from '../../utils/toast';
import { HasPermissionDirective } from '../../directives/has-permission.directive'; 

declare var bootstrap: any;

@Component({
  selector: 'app-reglas-puntos',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, HasPermissionDirective], 
  templateUrl: './reglas-puntos.html',
  styleUrls: ['./reglas-puntos.css']
})
export default class ReglasPuntosComponent implements OnInit {

  private reglaPuntosService = inject(ReglaPuntosService);
  private recompensaService = inject(RecompensaService); 
  private categoriaService = inject(CategoriaService); 
  private fb = inject(FormBuilder);

  public reglaActiva: ReglaPuntosDTO | null = null;
  public historialReglas: ReglaPuntosDTO[] = [];
  
  public listaRecompensas: RecompensaDTO[] = []; 
  
  public categorias: CategoriaDTO[] = []; 
  public isLoading = true; 
  public isSubmitting = false; 
  public isSubmittingRecompensa = false; 
  public errorMessage: string | null = null;
  public modalErrorMessage: string | null = null;
  public tipoDescuentoEnum = TipoDescuento; 

  public reglaForm: FormGroup; 
  public recompensaForm: FormGroup; 
  
  private recompensaModal: any; 
  public esEdicionRecompensa = false;
  public recompensaEditandoId: number | null = null;
  public aplicarA: 'TODO' | 'CATEGORIA' = 'TODO'; 

  constructor() {
    this.reglaForm = this.fb.group({
      descripcion: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(100)]],
      montoGasto: [1000, [Validators.required, Validators.min(1)]],
      puntosGanados: [100, [Validators.required, Validators.min(1)]],
      equivalenciaPuntos: [1, [Validators.min(0.01)]], 
      caducidadPuntosMeses: [12, [Validators.required, Validators.min(1), Validators.max(60)]]
    });

    this.recompensaForm = this.fb.group({
      descripcion: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(100)]],
      puntosRequeridos: [null, [Validators.required, Validators.min(1)]],
      stock: [null, [Validators.required, Validators.min(0)]], 
      tipoDescuento: [TipoDescuento.FIJO, [Validators.required]],
      valor: [null, [Validators.required, Validators.min(0.1)]],
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
    
    this.recompensaForm.get('tipoDescuento')?.valueChanges
      .pipe(distinctUntilChanged())
      .subscribe(tipo => {
        const categoriaControl = this.recompensaForm.get('categoriaId');
        if (tipo === TipoDescuento.PORCENTAJE) {
          categoriaControl?.enable();
        } else {
          this.aplicarA = 'TODO';
          categoriaControl?.disable();
          categoriaControl?.setValue(null);
        }
      });
  }
  
  cargarCategorias(): void {
     this.categoriaService.listarCategorias('ACTIVO').subscribe({
       next: (data) => this.categorias = data,
       error: () => mostrarToast('No se pudieron cargar las categorías.', 'danger')
     });
  }

  cargarDatos(): void {
    this.isLoading = true;
    this.errorMessage = null;

    // 1. Regla Activa
    this.reglaPuntosService.getReglaActiva().subscribe({
      next: (data) => {
        this.reglaActiva = data;
        if (data) {
          this.reglaForm.patchValue({
            descripcion: data.descripcion,
            montoGasto: data.montoGasto,
            puntosGanados: data.puntosGanados,
            equivalenciaPuntos: data.equivalenciaPuntos || 1,
            caducidadPuntosMeses: data.caducidadPuntosMeses
          });
        }
        this.isLoading = false; 
      },
      error: (err) => {
        if (err.status !== 404) { 
          this.errorMessage = "Error al cargar la regla activa.";
          mostrarToast(this.errorMessage!, 'danger');
        } else {
          this.reglaActiva = null;
        }
        this.isLoading = false;
      }
    });

    // 2. Historial
    this.reglaPuntosService.listarReglas().subscribe({
        next: (data) => this.historialReglas = data.filter(r => r.estadoRegla !== 'ACTIVA'),
        error: () => console.warn('Error historial')
    });

    // 3. Recompensas (INDEPENDIENTES)
    this.recompensaService.listarTodas().subscribe({
        next: (data) => {
            this.listaRecompensas = data;
        },
        error: () => mostrarToast('Error al cargar recompensas.', 'warning')
    });
  }

  onSubmitRegla(): void {
    this.reglaForm.markAllAsTouched();
    if (this.reglaForm.invalid) {
      mostrarToast('Revise los campos obligatorios de la regla.', 'warning');
      return;
    }
    if (!confirm("¿Crear nueva regla? La anterior será caducada.")) return;

    this.isSubmitting = true;
    
    const formValue = this.reglaForm.value;
    const nuevaReglaDTO: ReglaPuntosDTO = { 
      descripcion: formValue.descripcion,
      montoGasto: formValue.montoGasto,
      puntosGanados: formValue.puntosGanados,
      equivalenciaPuntos: formValue.equivalenciaPuntos, 
      caducidadPuntosMeses: formValue.caducidadPuntosMeses
    };
    
    this.reglaPuntosService.crearNuevaReglaActiva(nuevaReglaDTO).subscribe({
      next: () => {
        mostrarToast('¡Nueva regla activada!', 'success');
        this.isSubmitting = false;
        this.reglaForm.reset({ montoGasto: 1000, puntosGanados: 100, equivalenciaPuntos: 1, caducidadPuntosMeses: 12 });
        this.cargarDatos(); 
      },
      error: (err) => { 
        this.isSubmitting = false;
        mostrarToast('Error al crear regla.', 'danger');
      }
    });
  }

  // --- RECOMPENSAS ---

  abrirModalNuevaRecompensa(): void {
    this.esEdicionRecompensa = false;
    this.recompensaEditandoId = null;
    this.modalErrorMessage = null;
    this.aplicarA = 'TODO';
    this.recompensaForm.reset({
      stock: 10, 
      tipoDescuento: TipoDescuento.FIJO,
      categoriaId: null,
      puntosRequeridos: null,
      valor: null,
      descripcion: ''
    });
    this.recompensaForm.get('categoriaId')?.disable(); 
    this.recompensaModal.show();
  }

  abrirModalEditarRecompensa(recompensa: RecompensaDTO): void {
    this.esEdicionRecompensa = true;
    this.recompensaEditandoId = recompensa.id!;
    this.modalErrorMessage = null;

    if (recompensa.categoriaId) {
        this.aplicarA = 'CATEGORIA';
    } else {
        this.aplicarA = 'TODO';
    }

    this.recompensaForm.patchValue({
      descripcion: recompensa.descripcion,
      puntosRequeridos: recompensa.puntosRequeridos,
      stock: recompensa.stock, 
      tipoDescuento: recompensa.tipoDescuento,
      valor: recompensa.valor,
      categoriaId: recompensa.categoriaId || null
    });
    
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
    this.recompensaForm.markAllAsTouched();
    if (this.recompensaForm.invalid) {
      mostrarToast("Formulario inválido.", "warning");
      return;
    }
    if (this.aplicarA === 'CATEGORIA' && !this.recompensaForm.get('categoriaId')?.value) {
        mostrarToast("Debe seleccionar una categoría.", "warning");
        return;
    }

    this.isSubmittingRecompensa = true;
    this.modalErrorMessage = null;

    const recompensaData = this.recompensaForm.getRawValue() as RecompensaDTO;
    
    
    if (this.aplicarA === 'TODO' || recompensaData.tipoDescuento === TipoDescuento.FIJO) {
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
        this.cargarDatos(); // Recarga la lista independiente
      },
      error: (err: HttpErrorResponse) => {
        this.isSubmittingRecompensa = false;
        this.modalErrorMessage = err.error?.message || "Error al guardar la recompensa.";
      }
    });
  }

  eliminarRecompensa(id: number): void {
    if (!confirm("¿Eliminar recompensa?")) return;
    this.recompensaService.eliminar(id).subscribe({
      next: () => {
        mostrarToast("Eliminada.", 'success');
        this.cargarDatos();
      },
      error: () => mostrarToast("Error al eliminar.", 'danger')
    });
  }

  get fRegla() { return this.reglaForm.controls; }
  get fRec() { return this.recompensaForm.controls; }
}