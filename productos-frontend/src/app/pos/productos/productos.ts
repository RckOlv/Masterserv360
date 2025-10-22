import { Component, OnInit } from '@angular/core';
import { ProductoService } from '../../service/producto.service';
import { CategoriaService } from '../../service/categoria.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Producto } from '../../models/producto.model';
import { mostrarToast } from '../../utils/toast';

@Component({
  selector: 'app-productos',
  standalone: true,
  templateUrl: '../productos/productos.html',
  styleUrls: ['../productos/productos.css'],
  imports: [CommonModule, FormsModule],
})
export class ProductosComponent implements OnInit {
  productos: Producto[] = [];
  categorias: any[] = [];

  // 🔹 Filtros
  filtroNombre: string = '';
  categoriaSeleccionada: any = 0;
  activoSeleccionado: string = 'todos';
  fechaDesde: string = '';
  fechaHasta: string = '';
  mostrarFiltroAvanzado = false;

  // 🔹 Modal / formulario
  mostrarModal = false;
  esEdicion = false;
  nuevoProducto: Producto = this.crearProductoVacio();

  // 🔹 Paginación
  paginaActual = 1;
  totalPaginas = 1;

  constructor(
    private productoService: ProductoService,
    private categoriaService: CategoriaService
  ) {}

  ngOnInit(): void {
    this.listarProductos();
    this.cargarCategorias();
  }

 /** 🔹 Listar productos con filtros */
listarProductos(): void {
  // Determinar el valor booleano del filtro "activo"
  let activoParam: boolean | undefined;
  if (this.activoSeleccionado === 'activos') activoParam = true;
  else if (this.activoSeleccionado === 'inactivos') activoParam = false;

  // Si algún filtro está vacío, se envía como undefined
  const nombre = this.filtroNombre?.trim() || undefined;
  const categoriaId =
    this.categoriaSeleccionada && this.categoriaSeleccionada > 0
      ? this.categoriaSeleccionada
      : undefined;
  const desde = this.fechaDesde || undefined;
  const hasta = this.fechaHasta || undefined;

  // Llamada al servicio
  this.productoService
    .filtrarProductos(nombre, categoriaId, activoParam, desde, hasta)
    .subscribe({
      next: (res: Producto[]) => {
        this.productos = res.map(p => ({
          ...p,
          // Corrige el tipo, por si el backend devuelve boolean o string
          activo: String(p.activo ?? p.activo) === 'true' || p.activo === true,
        }));

        // Si el backend devuelve todos y el filtro es solo por activo, filtramos acá también
        if (activoParam !== undefined) {
          this.productos = this.productos.filter(p => p.activo === activoParam);
        }

        this.totalPaginas = Math.ceil(this.productos.length / 10);
        this.paginaActual = 1;
      },
      error: (err) => {
        console.error('Error al filtrar productos:', err);
        mostrarToast('Error al cargar productos', 'danger');
      },
    });
}

  /** 🔹 Cargar categorías */
  cargarCategorias(): void {
    this.categoriaService.listarCategorias().subscribe({
      next: (res: any) => (this.categorias = res),
      error: (err) => {
        console.error('Error al cargar categorías:', err);
        mostrarToast('Error al cargar categorías', 'danger');
      },
    });
  }

  /** 🔹 Limpiar filtros */
  limpiarFiltros(): void {
    this.resetFiltros();
    this.listarProductos();
  }

  /** 🔹 Abrir modal */
  toggleModal(producto?: Producto): void {
    this.esEdicion = !!producto;
    if (producto) {
      this.nuevoProducto = { ...producto };
      this.categoriaSeleccionada = producto.categoria?.idCategoria ?? 0;
    } else {
      this.nuevoProducto = this.crearProductoVacio();
      this.categoriaSeleccionada = 0;
    }
    this.mostrarModal = true;
  }

  /** 🔹 Guardar producto */
  guardarProducto(): void {
    if (
      !this.nuevoProducto.nombreProducto.trim() ||
      !this.categoriaSeleccionada ||
      this.categoriaSeleccionada === 0
    ) {
      mostrarToast('Debe ingresar un nombre y seleccionar una categoría.', 'danger');
      return;
    }

    const productoEnviar: Producto = {
      idProducto: this.nuevoProducto.idProducto,
      codigo: this.nuevoProducto.codigo,
      nombreProducto: this.nuevoProducto.nombreProducto,
      descripcion: this.nuevoProducto.descripcion,
      precioCosto: this.nuevoProducto.precioCosto,
      precioVenta: this.nuevoProducto.precioVenta,
      stockActual: this.nuevoProducto.stockActual,
      stockMinimo: this.nuevoProducto.stockMinimo,
      activo: this.nuevoProducto.activo, // 🧩 ya se maneja como boolean
      imagen: this.nuevoProducto.imagen,
      categoria: { idCategoria: this.categoriaSeleccionada },
    };

    const operacion = this.esEdicion
      ? this.productoService.actualizarProducto(productoEnviar)
      : this.productoService.crearProducto(productoEnviar);

    operacion.subscribe({
      next: () => {
        this.listarProductos();
        mostrarToast(
          this.esEdicion
            ? 'Producto actualizado correctamente'
            : 'Producto creado correctamente'
        );
        this.mostrarModal = false;
        this.resetFiltros();
      },
      error: (err) => {
        console.error('Error al guardar producto:', err);
        mostrarToast('Error al guardar producto', 'danger');
      },
    });
  }

  /** 🔹 Inactivar producto */
  eliminar(id: number) {
    if (confirm('¿Deseas inactivar este producto?')) {
      this.productoService.inactivarProducto(id).subscribe({
        next: () => {
          mostrarToast('Producto inactivado', 'warning');
          this.listarProductos();
        },
        error: (err) => console.error('Error al inactivar producto:', err),
      });
    }
  }

  /** 🔹 Reactivar producto */
  reactivar(id: number) {
    this.productoService.reactivarProducto(id).subscribe({
      next: () => {
        mostrarToast('Producto reactivado', 'success');
        this.listarProductos();
      },
      error: (err) => console.error('Error al reactivar producto:', err),
    });
  }

  /** 🔹 Paginación */
  get productosPaginados(): Producto[] {
    const inicio = (this.paginaActual - 1) * 10;
    return this.productos.slice(inicio, inicio + 10);
  }

  /** 🔹 Reset de filtros */
  private resetFiltros(): void {
    this.filtroNombre = '';
    this.categoriaSeleccionada = 0;
    this.activoSeleccionado = 'todos';
    this.fechaDesde = '';
    this.fechaHasta = '';
  }

  /** 🔹 Crear producto vacío */
  private crearProductoVacio(): Producto {
    return {
      idProducto: undefined,
      codigo: '',
      nombreProducto: '',
      descripcion: '',
      precioCosto: 0,
      precioVenta: 0,
      stockActual: 0,
      stockMinimo: 0,
      activo: true,
      imagen: undefined,
      categoria: { idCategoria: 0 },
    };
  }
}
