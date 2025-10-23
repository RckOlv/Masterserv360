import { Routes } from '@angular/router';
import { AuthGuard } from './guards/auth.guard'; // Importa nuestro guard

export const routes: Routes = [
  
  // --- Rutas Públicas (Layout Público) ---
  {
    path: '',
    // component: PublicLayoutComponent, // Tu layout público
    children: [
      { path: 'login', loadComponent: () => import('./pages/login/login') },
      { path: 'register', loadComponent: () => import('./pages/reg-cli/reg-cli') }, // Asumo que reg-cli es tu registro
      { path: '', redirectTo: 'login', pathMatch: 'full' }
    ]
  },

  // --- Rutas Protegidas (Layout de Admin/POS) ---
  {
    path: '', // O 'admin', 'pos', etc.
    // component: AdminLayoutComponent, // Tu layout de admin (con el sidebar)
    canActivate: [AuthGuard], // <-- ¡AQUÍ ESTÁ LA MAGIA!
    children: [
      { path: 'dashboard', loadComponent: () => import('./pos/dashboard/dashboard') },
      { path: 'productos', loadComponent: () => import('./pos/productos/productos') },
      { path: 'categorias', loadComponent: () => import('./pos/categorias/categorias') },
      { path: 'usuarios', loadComponent: () => import('./pos/usuarios-list/usuarios-list') },
      // ... más rutas protegidas ...
    ]
  },

  // Redirección general
  { path: '**', redirectTo: 'login' }
];