import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common'; // ¡Importante!
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms'; // ¡Importante!
import { PuntosService } from '../../service/puntos.service';
import { SaldoPuntosDTO } from '../../models/saldo-puntos.model';
import { CuponDTO } from '../../models/cupon.model';
import { mostrarToast } from '../../utils/toast';

@Component({
  selector: 'app-mis-puntos',
  standalone: true, // ¡Asegúrate de que sea standalone!
  imports: [
    CommonModule,
    ReactiveFormsModule // <-- Necesario para el formulario de canje
  ],
  templateUrl: './mis-puntos.html',
  styleUrl: './mis-puntos.css'
})
export default class MisPuntosComponent implements OnInit {

  // --- Inyección de Dependencias ---
  private fb = inject(FormBuilder);
  private puntosService = inject(PuntosService);

  // --- Estado del Componente ---
  public saldo: SaldoPuntosDTO | null = null;
  public cuponGenerado: CuponDTO | null = null; // Para mostrar el cupón
  
  public isLoadingSaldo = true;
  public isSubmitting = false; // Para el formulario de canje
  
  public canjeForm: FormGroup;

  constructor() {
    // Inicializamos el formulario de canje
    this.canjeForm = this.fb.group({
      // Los validadores se actualizarán dinámicamente
      puntosACanjear: [null, [Validators.required, Validators.min(1)]] 
    });
    this.canjeForm.disable(); // Deshabilitado hasta que cargue el saldo
  }

  ngOnInit(): void {
    this.loadSaldo();
  }

  /**
   * Carga el saldo de puntos (GET /api/puntos/mi-saldo)
   */
  loadSaldo(): void {
    this.isLoadingSaldo = true;
    this.puntosService.getMiSaldo().subscribe({
      next: (data) => {
        this.saldo = data;
        
        // --- ¡LÓGICA CLAVE! ---
        // Actualizamos los validadores del formulario con el saldo máximo
        const puntosControl = this.canjeForm.get('puntosACanjear');
        if (data.saldoPuntos > 0) {
          puntosControl?.setValidators([
            Validators.required,
            Validators.min(1),
            Validators.max(data.saldoPuntos) // No puede canjear más de lo que tiene
          ]);
          this.canjeForm.enable(); // Habilitamos el formulario
        } else {
          this.canjeForm.disable(); // Mantenemos deshabilitado si no hay puntos
        }
        puntosControl?.updateValueAndValidity(); // Aplicamos los nuevos validadores
        
        this.isLoadingSaldo = false;
      },
      error: (err) => {
        mostrarToast('Error al cargar el saldo de puntos', 'danger');
        this.isLoadingSaldo = false;
      }
    });
  }

  /**
   * Envía la solicitud de canje (POST /api/puntos/canjear)
   */
  onCanjear(): void {
    if (this.canjeForm.invalid) {
      // Los mensajes de error se mostrarán en el HTML
      this.canjeForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.cuponGenerado = null; // Ocultar cupón anterior
    const puntos = this.canjeForm.value.puntosACanjear;

    this.puntosService.canjearPuntos(puntos).subscribe({
      next: (cupon) => {
        mostrarToast('¡Puntos canjeados exitosamente!', 'success');
        this.cuponGenerado = cupon; // Mostramos el nuevo cupón
        this.isSubmitting = false;
        this.canjeForm.reset(); // Limpiamos el formulario
        this.loadSaldo(); // ¡Volvemos a cargar el saldo actualizado!
      },
      error: (err) => {
        // Leemos el mensaje de error del backend (ej. "Saldo insuficiente")
        const mensaje = err.error?.message || 'Error desconocido al canjear';
        mostrarToast(mensaje, 'danger');
        this.isSubmitting = false;
      }
    });
  }
}