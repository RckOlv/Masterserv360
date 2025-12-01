import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
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
      
      // MENTOR: Campos Nuevos
      // Iniciamos cantidad ofertada con la solicitada por defecto
      cantidadOfertada: [item.cantidadSolicitada, [Validators.required, Validators.min(1)]],
      precioUnitarioOfertado: [null, [Validators.required, Validators.min(0.01)]],
      disponible: [true] // Checkbox activado por defecto
    });
  }

  get itemsFormArray() {
    return this.form.get('items') as FormArray;
  }

  // MENTOR: Lógica para habilitar/deshabilitar según disponibilidad
  toggleDisponibilidad(index: number) {
    const group = this.itemsFormArray.at(index) as FormGroup;
    const isDisponible = group.get('disponible')?.value;

    if (isDisponible) {
        group.get('precioUnitarioOfertado')?.enable();
        group.get('cantidadOfertada')?.enable();
        // Restaurar cantidad solicitada por defecto si estaba vacío
        if (!group.get('cantidadOfertada')?.value) {
             const cantSol = group.get('cantidadSolicitada')?.value;
             group.get('cantidadOfertada')?.setValue(cantSol);
        }
    } else {
        group.get('precioUnitarioOfertado')?.disable();
        group.get('cantidadOfertada')?.disable();
        // Limpiamos valores para que no validen error
        group.get('precioUnitarioOfertado')?.setValue(null);
    }
  }

  onSubmit(): void {
    if (this.form.invalid) {
      mostrarToast('Por favor, complete todos los campos requeridos.', 'warning');
      this.form.markAllAsTouched(); 
      return;
    }
    
    if (!this.token) return; 

    this.isSubmitting = true;

    // Usamos getRawValue() para incluir los campos disabled (aunque aquí no los enviamos al back si no están disponibles, es buena práctica)
    const rawValue = this.form.getRawValue(); 
    
    const ofertaDTO: OfertaProveedorDTO = {
      fechaEntregaOfertada: rawValue.fechaEntregaOfertada,
      items: rawValue.items.map((item: any) => ({
        itemCotizacionId: item.itemCotizacionId,
        precioUnitarioOfertado: item.precioUnitarioOfertado,
        // Enviamos los nuevos campos al DTO
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