import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
// Importar FormsModule para [(ngModel)] y ReactiveFormsModule para [formGroup]
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ProveedorService } from '../../service/proveedor.service';
import { ProveedorDTO } from '../../models/proveedor.model';
import { CategoriaService } from '../../service/categoria.service'; // Para el multi-select
import { CategoriaDTO } from '../../models/categoria.model';   // Para el multi-select
import { mostrarToast } from '../../utils/toast';

// Declarar Bootstrap globalmente
declare var bootstrap: any;

@Component({
  selector: 'app-proveedores',
  standalone: true,
  // ¡Asegúrate de importar FormsModule y ReactiveFormsModule!
  imports: [CommonModule, ReactiveFormsModule, FormsModule], 
  templateUrl: './proveedores.html',
  styleUrls: ['./proveedores.css']
})
export default class ProveedoresComponent implements OnInit {

  private fb = inject(FormBuilder);
  private proveedorService = inject(ProveedorService);
  private categoriaService = inject(CategoriaService); // Inyectar

  // Estado
  proveedores: ProveedorDTO[] = []; // Lista completa (filtrada por estado)
  proveedoresFiltrados: ProveedorDTO[] = []; // Lista para la tabla (filtrada por nombre/cuit)
  categorias: CategoriaDTO[] = []; // Lista de categorías para el modal
  terminoBusqueda: string = '';
  filtroEstado: string = 'ACTIVO'; // Estado por defecto
  
  proveedorForm: FormGroup;
  esEdicion = false;
  proveedorEditId: number | null = null;
  isLoading = false;
  errorMessage: string | null = null;

  constructor() {
    this.proveedorForm = this.fb.group({
      id: [null],
      razonSocial: ['', [Validators.required, Validators.maxLength(255)]],
      cuit: ['', [Validators.required, Validators.maxLength(20)]],
      email: ['', [Validators.email, Validators.maxLength(100)]],
      telefono: ['', [Validators.maxLength(20)]],
      direccion: ['', [Validators.maxLength(255)]],
      estado: ['ACTIVO'],
      categoriaIds: [[]] // Para el multi-select de categorías
    });
  }

  ngOnInit() {
    this.listarProveedores();
    this.cargarCategorias(); // Cargar categorías para el modal
  }

  /** 🔹 Carga categorías (para el modal) */
  cargarCategorias(): void {
    // Llama al servicio de categorías (que ya filtra por 'ACTIVO' por defecto)
    this.categoriaService.listarCategorias('ACTIVO').subscribe({
      next: (data) => this.categorias = data,
      error: (err: any) => console.error('Error al cargar categorías', err)
    });
  }

  /** 🔹 Carga proveedores desde el backend (filtrado por estado) */
  listarProveedores() {
    this.isLoading = true;
    this.errorMessage = null;
    
    this.proveedorService.listarProveedores(this.filtroEstado).subscribe({
      next: (data: ProveedorDTO[]) => {
        this.proveedores = data;
        this.filtrarLocalmente(); // Aplicar filtro de búsqueda local
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error('Error al listar proveedores:', err);
        this.errorMessage = 'Error al cargar proveedores.';
        if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
        this.isLoading = false;
      },
    });
  }

  /** 🔹 Se llama cuando cambian los filtros */
  aplicarFiltros(): void {
    // Si cambia el estado, recarga del backend
    // Si cambia el término de búsqueda, filtra localmente
    this.listarProveedores();
  }

  /** 🔹 Filtra localmente por Razón Social o CUIT */
  filtrarLocalmente() {
    const termino = this.terminoBusqueda.toLowerCase().trim();
    if (!termino) {
      this.proveedoresFiltrados = [...this.proveedores];
    } else {
      this.proveedoresFiltrados = this.proveedores.filter((p) => 
        p.razonSocial.toLowerCase().includes(termino) || 
        (p.cuit && p.cuit.toLowerCase().includes(termino))
      );
    }
  }

  reiniciarFiltros() {
    this.terminoBusqueda = '';
    this.filtroEstado = 'ACTIVO'; 
    this.listarProveedores(); 
    mostrarToast('Filtros reiniciados');
  }

  abrirModalNuevo() {
    this.esEdicion = false;
    this.proveedorEditId = null;
    this.proveedorForm.reset({
      id: null,
      razonSocial: '',
      cuit: '',
      email: '',
      telefono: '',
      direccion: '',
      estado: 'ACTIVO',
      categoriaIds: [] // Resetea el multi-select
    });
    this.errorMessage = null;
    const modal = new bootstrap.Modal(document.getElementById('proveedorModal'));
    modal.show();
  }

  abrirModalEditar(proveedor: ProveedorDTO) {
    if (!proveedor.id) return;
    this.esEdicion = true;
    this.proveedorEditId = proveedor.id;
    // Usamos el servicio 'getById' para asegurarnos de tener los 'categoriaIds'
    this.proveedorService.getById(proveedor.id).subscribe({
      next: (data) => {
        this.proveedorForm.patchValue(data); // Carga todos los datos, incluyendo categoriaIds
        this.errorMessage = null;
        const modal = new bootstrap.Modal(document.getElementById('proveedorModal'));
        modal.show();
      },
      error: (err: any) => {
        mostrarToast('Error al cargar datos del proveedor', 'danger');
        this.errorMessage = err.error?.message;
      }
    });
  }

  guardarProveedor() {
    this.proveedorForm.markAllAsTouched();
    if (this.proveedorForm.invalid) {
      mostrarToast("Revise los campos obligatorios.", "warning");
      return;
    }

    this.isLoading = true;
    this.errorMessage = null;
    const proveedorData = this.proveedorForm.value as ProveedorDTO;
    
    const obs = this.esEdicion
      ? this.proveedorService.actualizar(this.proveedorEditId!, proveedorData)
      : this.proveedorService.crear(proveedorData);

    obs.subscribe({
      next: (guardado: ProveedorDTO) => {
        this.listarProveedores(); // Recarga la lista
        mostrarToast(this.esEdicion ? 'Proveedor actualizado' : 'Proveedor creado', 'success');
        this.cerrarModal();
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error('Error al guardar proveedor:', err);
        this.errorMessage = err.error?.message || 'Error al guardar el proveedor.';
        if (this.errorMessage) mostrarToast(this.errorMessage, 'danger');
        this.isLoading = false;
      },
    });
  }

  cerrarModal() {
    const modalElement = document.getElementById('proveedorModal');
    if (modalElement) {
      const modal = bootstrap.Modal.getInstance(modalElement);
      if (modal) modal.hide();
    }
  }

  eliminarProveedor(id?: number) {
    if (!id) return;
    if (confirm('¿Estás seguro de marcar este proveedor como INACTIVO?')) {
      this.isLoading = true;
      this.proveedorService.softDelete(id).subscribe({
        next: () => {
          this.listarProveedores(); 
          mostrarToast('Proveedor marcado como inactivo', 'warning');
        },
        error: (err: any) => {
          console.error('Error al eliminar proveedor:', err);
          if (err.error?.message) mostrarToast(err.error.message, 'danger');
          this.isLoading = false;
        },
      });
    }
  }

  reactivarProveedor(id?: number) {
     if (!id) return;
     if (confirm('¿Estás seguro de REACTIVAR este proveedor?')) {
       this.isLoading = true;
       this.proveedorService.reactivar(id).subscribe({
         next: () => {
           this.listarProveedores(); 
           mostrarToast('Proveedor reactivado', 'success');
         },
         error: (err: any) => {
           console.error('Error al reactivar proveedor:', err);
           if (err.error?.message) mostrarToast(err.error.message, 'danger');
           this.isLoading = false;
         },
       });
     }
  }
    
  obtenerNombreCategoria(catId: number): string {
    const categoria = this.categorias.find(c => c.id === catId);
    return categoria ? categoria.nombre : `ID: ${catId}`;
  }

  // Helper para validación
  get f() { return this.proveedorForm.controls; }
}