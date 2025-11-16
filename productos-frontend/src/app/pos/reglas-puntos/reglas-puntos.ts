import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ReglaPuntosService } from '../../service/regla-puntos.service';
import { ReglaPuntosDTO } from '../../models/regla-puntos.model';
import { mostrarToast } from '../../utils/toast';

// --- Mentor: INICIO DE LA MODIFICACIÓN ---
// 1. Importar la nueva directiva de permisos
import { HasPermissionDirective } from '../../directives/has-permission.directive'; 
// --- Mentor: FIN DE LA MODIFICACIÓN ---

@Component({
  selector: 'app-reglas-puntos',
  standalone: true,
  imports: [
    CommonModule, 
    ReactiveFormsModule,
    // --- Mentor: INICIO DE LA MODIFICACIÓN ---
    // 2. Añadir la directiva a los imports del componente
    HasPermissionDirective
    // --- Mentor: FIN DE LA MODIFICACIÓN ---
  ], 
  templateUrl: './reglas-puntos.html',
  styleUrls: ['./reglas-puntos.css']
})
export default class ReglasPuntosComponent implements OnInit {

  // --- Inyección de Dependencias ---
  private reglaPuntosService = inject(ReglaPuntosService);
  private fb = inject(FormBuilder);

  // --- Estado del Componente ---
  public reglaActiva: ReglaPuntosDTO | null = null;
  public historialReglas: ReglaPuntosDTO[] = [];
  public isLoading = true; 
  public isSubmitting = false;
  public errorMessage: string | null = null;

  // --- Formulario ---
  public reglaForm: FormGroup;

  constructor() {
    this.reglaForm = this.fb.group({
      descripcion: ['', [Validators.required, Validators.maxLength(100)]],
      montoGasto: [1000, [Validators.required, Validators.min(1)]],
      puntosGanados: [100, [Validators.required, Validators.min(1)]],
      equivalenciaPuntos: [1, [Validators.required, Validators.min(0.01)]],
      caducidadPuntosMeses: [12, [Validators.required, Validators.min(1)]]
    });
  }

  ngOnInit(): void {
    this.cargarDatos();
  }

  /** Carga la regla activa y el historial */
  cargarDatos(): void {
    this.isLoading = true;
    this.errorMessage = null;

    // Cargar la regla activa
    this.reglaPuntosService.getReglaActiva().subscribe({
      next: (data) => {
        this.reglaActiva = data;
        if (data) {
          this.reglaForm.patchValue({
            descripcion: data.descripcion,
            montoGasto: data.montoGasto,
            puntosGanados: data.puntosGanados,
            equivalenciaPuntos: data.equivalenciaPuntos,
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

    // Cargar el historial
    this.reglaPuntosService.listarReglas().subscribe({
        next: (data) => {
          this.historialReglas = data.filter(r => r.estadoRegla !== 'ACTIVA');
        },
        error: () => mostrarToast('Error al cargar historial de reglas.', 'warning')
    });
  }

  /** Envía el formulario para crear la nueva regla activa */
  onSubmit(): void {
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

    const nuevaReglaDTO: any = { 
      descripcion: formValue.descripcion,
      montoGasto: formValue.montoGasto,
      puntosGanados: formValue.puntosGanados,
      equivalenciaPuntos: formValue.equivalenciaPuntos,
      caducidadPuntosMeses: formValue.caducidadPuntosMeses,
    };
    
    console.log("Enviando este objeto al backend:", nuevaReglaDTO);

    this.reglaPuntosService.crearNuevaReglaActiva(nuevaReglaDTO as ReglaPuntosDTO).subscribe({
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
        this.cargarDatos();
      },
      error: (err) => {
        console.error("Error al crear regla:", err);
        this.isSubmitting = false;
        
        let mensaje = "No se pudo crear la nueva regla. Verifique los datos.";

        if (err.status === 400 && err.error?.errors && Array.isArray(err.error.errors)) {
          mensaje = err.error.errors.map((e: any) => e.defaultMessage).join('; ');
        } else if (err.error?.message) {
          mensaje = err.error.message;
        }
        
        this.errorMessage = mensaje;
        mostrarToast(this.errorMessage, 'danger');
      }
    });
  }
}