// src/app/app.routes.ts
import { Routes } from '@angular/router';
import { AuthGuard } from './guards/auth.guard';
// Importa tus layouts si los usas como componentes y no solo como directorios
// import PublicLayoutComponent from './layouts/public-layout/public-layout.component';
// import AdminLayoutComponent from './layouts/admin-layout/admin-layout.component';

export const routes: Routes = [

  // --- Rutas Públicas ---
  {
    path: '',
    // component: PublicLayoutComponent, // Descomenta si usas un layout como componente
    children: [
      { path: 'login', loadComponent: () => import('./pages/login/login') },
      { path: 'register', loadComponent: () => import('./pages/reg-cli/reg-cli') }, // Registro de Clientes
      // Redirige la ruta vacía a login por defecto
      { path: '', redirectTo: 'login', pathMatch: 'full' }
    ]
  },

  // --- Rutas Protegidas (POS / Admin) ---
  {
    path: '', // Ruta base protegida (ej. /dashboard, /productos, etc.)
    // component: AdminLayoutComponent, // Descomenta si usas un layout como componente
    canActivate: [AuthGuard], // <-- Protege TODOS los hijos
    children: [
      { path: 'dashboard', loadComponent: () => import('./pos/dashboard/dashboard') },

      // Módulo Productos
      { path: 'productos', loadComponent: () => import('./pos/productos/productos') },
      { path: 'productos/nuevo', loadComponent: () => import('./pos/producto-form/producto-form') },
      { path: 'productos/editar/:id', loadComponent: () => import('./pos/producto-form/producto-form') },

      // Módulo Categorías
      { path: 'categorias', loadComponent: () => import('./pos/categorias/categorias') },

      // Módulo Usuarios (Admin)
      { path: 'usuarios', loadComponent: () => import('./pos/usuarios-list/usuarios-list') },
      {
        path: 'usuarios/nuevo',
        loadComponent: () => import('./layouts/admin-layout/registro/registro')
      },
      {
        path: 'usuarios/editar/:id',
        loadComponent: () => import('./layouts/admin-layout/registro/registro')
      },

      // Módulo Permisos (Admin)
      {
        path: 'permisos',
        loadComponent: () => import('./pos/permisos/permisos')
      },

       // Módulo Roles (Admin)
      {
        path: 'roles',
        loadComponent: () => import('./pos/roles/roles')
      },

      // Módulo Proveedores
      {
        path: 'proveedores',
        loadComponent: () => import('./pos/proveedores/proveedores')
      },

      // Módulo Pedidos
      {
        path: 'pedidos',
        loadComponent: () => import('./pos/pedidos-list/pedidos-list')
      },
      {
        path: 'pedidos/nuevo',
        loadComponent: () => import('./pos/pedido-form/pedido-form')
      },

      // --- ¡RUTA PARA PUNTO DE VENTA AÑADIDA! ---
      {
        path: 'ventas', // <-- La URL principal para el POS / Ventas
        loadComponent: () => import('./pos/punto-venta/punto-venta')
      },
      // ------------------------------------------

      // --- Ruta para el Carrito (Si quieres mantenerla separada) ---
      // Si el PuntoVentaComponent ya incluye todo, esta ruta 'carrito'
      // podría no ser necesaria o redirigir a 'ventas'.
      // Por ahora la dejamos como estaba antes.
      {
        path: 'carrito', // <-- La URL será /carrito
        loadComponent: () => import('./pos/carrito/carrito') // OJO: ¿Es este el componente correcto ahora?
      },
      // -------------------------------------------------------------


      // Redirección por defecto para rutas protegidas
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }

    ]
  },

  // Redirección general para rutas no encontradas
  { path: '**', redirectTo: 'login' } // O a una página '404 Not Found'
];