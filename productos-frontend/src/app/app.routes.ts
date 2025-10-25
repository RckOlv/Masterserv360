// src/app/app.routes.ts
import { Routes } from '@angular/router';
import { AuthGuard } from './guards/auth.guard'; 

export const routes: Routes = [
  
  // --- Rutas Públicas ---
  {
    path: '',
    // component: PublicLayoutComponent, // Tu layout público
    children: [
      { path: 'login', loadComponent: () => import('./pages/login/login') },
      { path: 'register', loadComponent: () => import('./pages/reg-cli/reg-cli') }, // Registro de Clientes
      { path: '', redirectTo: 'login', pathMatch: 'full' }
    ]
  },

  // --- Rutas Protegidas ---
  {
    path: '', // Ruta base protegida
    // component: AdminLayoutComponent, // Tu layout de admin
    canActivate: [AuthGuard], // <-- Protege TODOS los hijos
    children: [
      { path: 'dashboard', loadComponent: () => import('./pos/dashboard/dashboard') },
      
      // Módulo Productos
      { path: 'productos', loadComponent: () => import('./pos/productos/productos') },
      { path: 'productos/nuevo', loadComponent: () => import('./pos/producto-form/producto-form') },
      { path: 'productos/editar/:id', loadComponent: () => import('./pos/producto-form/producto-form') },
      
      // Módulo Categorías
      { path: 'categorias', loadComponent: () => import('./pos/categorias/categorias') },

      // Módulo Usuarios
      { path: 'usuarios', loadComponent: () => import('./pos/usuarios-list/usuarios-list') },
      
      // --- ¡AQUÍ ESTÁ LA CORRECCIÓN! ---
      { 
        path: 'usuarios/nuevo', // <- La ruta para crear (antes '/admin/registro')
        loadComponent: () => import('./layouts/admin-layout/registro/registro') 
      },
      { 
        path: 'usuarios/editar/:id', // <- La ruta para editar
        loadComponent: () => import('./layouts/admin-layout/registro/registro') 
      }
    ]
  },

  // Redirección general
  { path: '**', redirectTo: 'login' }
];