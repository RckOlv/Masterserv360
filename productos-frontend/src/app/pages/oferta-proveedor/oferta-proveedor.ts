import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators, AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
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
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './oferta-proveedor.html',
  styleUrls: ['./oferta-proveedor.css']
})
export default class OfertaProveedorComponent implements OnInit {

  private publicService = inject(PublicService);
  private route = inject(ActivatedRoute);
  private fb = inject(FormBuilder);
  private cdr = inject(ChangeDetectorRef);

  public token: string | null = null;
  public cotizacion: CotizacionPublicaDTO | null = null;
  public form: FormGroup;

  // Estados de UI
  public isLoading = true;
  public isSubmitting = false;
  public isSuccess = false; 
  public errorMessage: string | null = null;

  // Validaciones
  public minDate: string = ''; // Fecha mínima para el HTML

  constructor() {
    // Calcular fecha de hoy para el min del calendario (YYYY-MM-DD)
    const today = new Date();
    const tzOffset = today.getTimezoneOffset() * 60000; // Ajuste zona horaria
    this.minDate = (new Date(today.getTime() - tzOffset)).toISOString().split("T")[0];

    this.form = this.fb.group({
      fechaEntregaOfertada: [null, [Validators.required, this.fechaMinimaValidator(this.minDate)]],
      items: this.fb.array([])
    });
  }

  ngOnInit(): void {
    this.token = this.route.snapshot.paramMap.get('token');
    
    if (!this.token) {
      this.isLoading = false;
      this.errorMessage = 'Token inválido o faltante.';
      this.cdr.detectChanges();
      return;
    }

    this.publicService.getCotizacionPorToken(this.token).subscribe({
      next: (data) => {
        this.cotizacion = data;
        this.buildForm(data); 
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading = false;
        if (err.status === 404 || err.status === 410) {
          this.errorMessage = err.error.message;
        } else {
          this.errorMessage = 'Error al cargar la solicitud.';
        }
        this.cdr.detectChanges();
      }
    });
  }

  // Validador personalizado para la fecha
  fechaMinimaValidator(minDateStr: string): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
        if (!control.value) return null;
        // Comparación simple de strings YYYY-MM-DD funciona bien
        return control.value < minDateStr ? { fechaPasada: true } : null;
    };
  }

  private buildForm(cotizacion: CotizacionPublicaDTO): void {
    cotizacion.items.forEach(item => {
      this.itemsFormArray.push(this.createItemControl(item));
    });
  }

  private createItemControl(item: ItemCotizacionPublicoDTO): FormGroup {
    return this.fb.group({
      itemCotizacionId: [item.id], 
      productoNombre: [item.productoNombre],
      productoCodigo: [item.productoCodigo],
      cantidadSolicitada: [item.cantidadSolicitada],
      
      // MENTOR: Agregado Validators.max con la cantidad solicitada
      cantidadOfertada: [
          item.cantidadSolicitada, 
          [Validators.required, Validators.min(1), Validators.max(item.cantidadSolicitada)]
      ],
      precioUnitarioOfertado: [null, [Validators.required, Validators.min(0.01)]],
      disponible: [true] 
    });
  }

  get itemsFormArray() {
    return this.form.get('items') as FormArray;
  }

  // Getter para el template
  get f() { return this.form.controls; }

  toggleDisponibilidad(index: number) {
    const group = this.itemsFormArray.at(index) as FormGroup;
    const isDisponible = group.get('disponible')?.value;

    if (isDisponible) {
        group.get('precioUnitarioOfertado')?.enable();
        group.get('cantidadOfertada')?.enable();
        if (!group.get('cantidadOfertada')?.value) {
             const cantSol = group.get('cantidadSolicitada')?.value;
             group.get('cantidadOfertada')?.setValue(cantSol);
        }
    } else {
        group.get('precioUnitarioOfertado')?.disable();
        group.get('cantidadOfertada')?.disable();
        group.get('precioUnitarioOfertado')?.setValue(null);
    }
  }

  onSubmit(): void {
    this.form.markAllAsTouched();

    if (this.form.invalid) {
      mostrarToast('Por favor, verifique los datos ingresados (fechas o cantidades).', 'warning');
      return;
    }
    
    if (!this.token) return; 

    this.isSubmitting = true;

    const rawValue = this.form.getRawValue(); 
    
    const ofertaDTO: OfertaProveedorDTO = {
      fechaEntregaOfertada: rawValue.fechaEntregaOfertada,
      items: rawValue.items.map((item: any) => ({
        itemCotizacionId: item.itemCotizacionId,
        precioUnitarioOfertado: item.precioUnitarioOfertado,
        cantidadOfertada: item.cantidadOfertada,
        disponible: item.disponible
      }))
    };

    this.publicService.submitOferta(this.token, ofertaDTO).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.isSuccess = true; 
        this.form.disable(); 
        this.cdr.detectChanges();
      },
      error: (err: HttpErrorResponse) => {
        const errorMsg = err.error?.message || 'Error al enviar la oferta.';
        mostrarToast(errorMsg, 'danger');
        this.isSubmitting = false;
        this.cdr.detectChanges();
      }
    });
  }
}