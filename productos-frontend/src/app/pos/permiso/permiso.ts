import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { RouterModule } from '@angular/router';
import { PermisoService } from '../../service/permiso.service';
import { Permiso } from '../../models/permiso.model';

@Component({
  selector: 'app-permiso',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, HttpClientModule, RouterModule],
  templateUrl: './permiso.html',
  styleUrls: ['./permiso.css']
})
export class PermisoComponent implements OnInit {
  permisos: Permiso[] = [];
  permisoForm!: FormGroup;
  editMode: boolean = false;
  permisoEditId?: number;

  constructor(private fb: FormBuilder, private permisoService: PermisoService) {}

  ngOnInit() {
    this.cargarPermisos();
    this.permisoForm = this.fb.group({
      nombre: ['', Validators.required],
      descripcion: ['']
    });
  }

  cargarPermisos() {
    this.permisoService.listarPermisos().subscribe({
      next: (data) => this.permisos = data,
      error: (err) => console.error(err)
    });
  }

  guardarPermiso() {
    if (this.permisoForm.invalid) {
      this.permisoForm.markAllAsTouched();
      return;
    }

    const permiso: Permiso = this.permisoForm.value;

    if (this.editMode && this.permisoEditId != null) {
      permiso.id = this.permisoEditId;
      this.permisoService.actualizarPermiso(permiso).subscribe(() => {
        this.resetForm();
        this.cargarPermisos();
      });
    } else {
      this.permisoService.crearPermiso(permiso).subscribe(() => {
        this.resetForm();
        this.cargarPermisos();
      });
    }
  }

  editarPermiso(permiso: Permiso) {
    this.editMode = true;
    this.permisoEditId = permiso.id;
    this.permisoForm.patchValue({
      nombre: permiso.nombre,
      descripcion: permiso.descripcion
    });
  }

  eliminarPermiso(id: number) {
    if (confirm('¿Seguro que deseas eliminar este permiso?')) {
      this.permisoService.eliminarPermiso(id).subscribe(() => this.cargarPermisos());
    }
  }

  resetForm() {
    this.permisoForm.reset();
    this.editMode = false;
    this.permisoEditId = undefined;
  }
}
