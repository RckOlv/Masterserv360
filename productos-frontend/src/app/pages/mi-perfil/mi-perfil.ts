import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ClienteService } from '../../service/cliente.service';
import { TipoDocumentoService } from '../../service/tipo-documento.service';
import { ClientePerfilUpdateDTO } from '../../models/cliente-perfil-update.model';
import { TipoDocumentoDTO } from '../../models/tipo-documento.model';
import { mostrarToast } from '../../utils/toast';

@Component({
  selector: 'app-mi-perfil',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule], 
  templateUrl: './mi-perfil.html',
  styleUrl: './mi-perfil.css'
})
export default class MiPerfilComponent implements OnInit {

  // --- Inyección de Dependencias ---
  private fb = inject(FormBuilder);
  private clienteService = inject(ClienteService);
  private tipoDocumentoService = inject(TipoDocumentoService); // Para el <select>

  // --- Estado del Componente ---
  public perfilForm: FormGroup;
  public tiposDocumento: TipoDocumentoDTO[] = [];
  public isLoading = true;
  public isSubmitting = false;

  constructor() {
    // Inicializamos el formulario (vacío y deshabilitado)
    this.perfilForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.maxLength(100)]],
      apellido: ['', [Validators.required, Validators.maxLength(100)]],
      telefono: ['', [Validators.maxLength(20)]],
      tipoDocumentoId: [null, [Validators.required]],
      documento: ['', [Validators.required, Validators.maxLength(20)]]
    });
    // Lo deshabilitamos hasta que carguen los datos
    this.perfilForm.disable();
  }

  ngOnInit(): void {
    // Cargamos los datos en paralelo
    this.loadTiposDocumento();
    this.loadPerfil();
  }

  /**
   * Carga la lista de Tipos de Documento para el <select>
   */
  loadTiposDocumento(): void {
    // (Tu lógica aquí es perfecta)
    this.tipoDocumentoService.listarTiposDocumento().subscribe({
      next: (data) => {
        this.tiposDocumento = data;
      },
      error: (err) => {
        mostrarToast('Error al cargar los tipos de documento', 'danger');
      }
    });
  }

  /**
   * Carga los datos del perfil del cliente (GET /api/cliente/mi-perfil)
   */
  loadPerfil(): void {
    this.isLoading = true;
    this.clienteService.getMiPerfil().subscribe({
      next: (data) => {
        // Rellenamos el formulario con los datos recibidos
        this.perfilForm.patchValue({
          nombre: data.nombre,
          apellido: data.apellido,
          telefono: data.telefono,
          
          // --- ¡AQUÍ ESTÁ LA CORRECCIÓN! ---
          // data.id es el ID del Usuario.
          // data.tipoDocumentoId es el ID del Tipo de Documento.
          tipoDocumentoId: data.tipoDocumentoId, 
          // -------------------------------
          
          documento: data.documento
        });
        this.perfilForm.enable(); // Habilitamos el formulario
        this.isLoading = false;
      },
      error: (err) => {
        mostrarToast('Error fatal al cargar el perfil', 'danger');
        this.isLoading = false;
      }
    });
  }

  /**
   * Envía los datos actualizados (PUT /api/cliente/mi-perfil)
   */
  onSubmit(): void {
    // (Tu lógica de onSubmit está perfecta)
    if (this.perfilForm.invalid) {
      mostrarToast('Formulario inválido. Revise los campos.', 'warning');
      this.perfilForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    
    // Creamos el DTO de actualización solo con los valores del formulario
    const updateDTO: ClientePerfilUpdateDTO = this.perfilForm.value;

    this.clienteService.updateMiPerfil(updateDTO).subscribe({
      next: (perfilActualizado) => {
        mostrarToast('¡Perfil actualizado con éxito!', 'success');
        this.isSubmitting = false;
        
        // (Opcional) Volvemos a rellenar el form por si el backend cambió algo
        // Hacemos patchValue en lugar de 'perfilActualizado' 
        // porque perfilActualizado (ClientePerfilDTO) tiene 'tipoDocumento' (string)
        // y el formulario necesita 'tipoDocumentoId' (number).
        this.perfilForm.patchValue(updateDTO); 
      },
      error: (err) => {
        this.isSubmitting = false;
        // (Mejora: leer el 'err.error.message' del backend)
        mostrarToast('No se pudo actualizar el perfil', 'danger');
      }
    });
  }
}