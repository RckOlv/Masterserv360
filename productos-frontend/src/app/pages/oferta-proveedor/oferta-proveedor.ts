import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core'; // ¡1. Importar ChangeDetectorRef!
import { CommonModule, CurrencyPipe } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { ActivatedRoute, Router , RouterLink} from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

// Modelos y Servicios
import { PublicService } from '../../service/public.service';
import { CotizacionPublicaDTO } from '../../models/cotizacion-publica.model';
import { OfertaProveedorDTO } from '../../models/oferta-proveedor.model';
import { ItemCotizacionPublicoDTO } from '../../models/item-cotizacion-publico.model';

// Utils
import { mostrarToast } from '../../utils/toast';

@Component({
  selector: 'app-oferta-proveedor',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule,RouterLink],
  templateUrl: './oferta-proveedor.html',
  styleUrls: ['./oferta-proveedor.css'] // <-- Este CSS forzará el tema oscuro
})
export default class OfertaProveedorComponent implements OnInit {

  private publicService = inject(PublicService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef); // ¡2. Inyectar el ChangeDetectorRef!

  public token: string | null = null;
  public cotizacion: CotizacionPublicaDTO | null = null;
  public form: FormGroup;

  // Estados de UI
  public isLoading = true;
  public isSubmitting = false;
  public isSuccess = false; 
  public errorMessage: string | null = null;

  constructor() {
    this.form = this.fb.group({
      fechaEntregaOfertada: [null, [Validators.required]],
      items: this.fb.array([])
    });
  }

  ngOnInit(): void {
    this.token = this.route.snapshot.paramMap.get('token');
    
    if (!this.token) {
      this.isLoading = false;
      this.errorMessage = 'Token inválido o faltante.';
      this.cdr.detectChanges(); // ¡3. Forzar detección de cambios!
      return;
    }

    this.publicService.getCotizacionPorToken(this.token).subscribe({
      next: (data) => {
        this.cotizacion = data;
        this.buildForm(data); 
        this.isLoading = false;
        this.cdr.detectChanges(); // ¡3. Forzar detección de cambios!
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading = false;
        if (err.status === 404 || err.status === 410) {
          this.errorMessage = err.error.message;
        } else {
          this.errorMessage = 'Error al cargar la solicitud.';
        }
        this.cdr.detectChanges(); // ¡3. Forzar detección de cambios!
      }
    });
  }

  /**
   * Construye el FormArray dinámico basado en los items recibidos
   */
  private buildForm(cotizacion: CotizacionPublicaDTO): void {
    cotizacion.items.forEach(item => {
      this.itemsFormArray.push(this.createItemControl(item));
    });
  }

  /**
   * Helper para crear un FormGroup para un solo item
   */
  private createItemControl(item: ItemCotizacionPublicoDTO): FormGroup {
    return this.fb.group({
      itemCotizacionId: [item.id], 
      productoNombre: [{ value: item.productoNombre, disabled: true }],
      cantidadSolicitada: [{ value: item.cantidadSolicitada, disabled: true }],
      precioUnitarioOfertado: [null, [Validators.required, Validators.min(0.01)]]
    });
  }

  // Getter para acceder fácilmente al FormArray desde el HTML
  get itemsFormArray() {
    return this.form.get('items') as FormArray;
  }

  /**
   * Se llama al enviar el formulario
   */
  onSubmit(): void {
    if (this.form.invalid) {
      mostrarToast('Por favor, complete todos los precios y la fecha de entrega.', 'warning');
      this.form.markAllAsTouched(); 
      return;
    }
    
    if (!this.token) return; 

    this.isSubmitting = true;

    const rawValue = this.form.getRawValue(); 
    const ofertaDTO: OfertaProveedorDTO = {
      fechaEntregaOfertada: rawValue.fechaEntregaOfertada,
      items: rawValue.items.map((item: any) => ({
        itemCotizacionId: item.itemCotizacionId,
        precioUnitarioOfertado: item.precioUnitarioOfertado
      }))
    };

    this.publicService.submitOferta(this.token, ofertaDTO).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.isSuccess = true; 
        this.form.disable(); 
        this.cdr.detectChanges(); // ¡3. Forzar detección de cambios!
      },
      error: (err: HttpErrorResponse) => {
        const errorMsg = err.error?.message || 'Error al enviar la oferta.';
        mostrarToast(errorMsg, 'danger');
        this.isSubmitting = false;
        this.cdr.detectChanges(); // ¡3. Forzar detección de cambios!
      }
    });
  }
}