import { Routes } from '@angular/router';
import { AuthGuard } from './guards/auth.guard';
import { LoginGuard } from './guards/login.guard';

// Importamos los 3 layouts
import { AdminLayoutComponent } from './layouts/admin-layout/admin-layout';
import { PublicLayoutComponent } from './layouts/public-layout/public-layout';
import AuthLayoutComponent from './layouts/auth-layout/auth-layout';

export const routes: Routes = [

  // --- MUNDO PÚBLICO (LOGIN / REGISTER) ---
  {
    path: '', 
    component: AuthLayoutComponent, 
    canActivate: [LoginGuard], 
    children: [
      { path: 'login', loadComponent: () => import('./pages/login/login') },
      { path: 'register', loadComponent: () => import('./pages/reg-cli/reg-cli') },
      { path: '', redirectTo: 'login', pathMatch: 'full' }
    ]
  },

  // --- MUNDO INTERNO (ADMIN / VENDEDOR) ---
  {
    path: 'pos', 
    component: AdminLayoutComponent, 
    canActivate: [AuthGuard],
    canActivateChild: [AuthGuard],
    data: { 
      // Esta es la regla "padre": Vendedor puede entrar a /pos
      roles: ['ROLE_ADMIN', 'ROLE_VENDEDOR'] 
    },
    children: [
      // --- Rutas de VENDEDOR (y Admin) ---
      { path: 'dashboard', loadComponent: () => import('./pos/dashboard/dashboard') },
      { path: 'punto-venta', loadComponent: () => import('./pos/punto-venta/punto-venta') },
      { path: 'ventas-historial', loadComponent: () => import('./pos/ventas-list/ventas-list') },
      { path: 'ventas/:id', loadComponent: () => import('./pos/venta-detalle/venta-detalle') },
      
      // --- PRODUCTOS (Lista y Formulario) ---
      { path: 'productos', loadComponent: () => import('./pos/productos/productos') },
      // 🚀 NUEVAS RUTAS AGREGADAS:
      { 
        path: 'productos/nuevo', 
        loadComponent: () => import('./pos/producto-form/producto-form') 
      },
      { 
        path: 'productos/editar/:id', 
        loadComponent: () => import('./pos/producto-form/producto-form') 
      },
      // --------------------------------------

      { path: 'categorias', loadComponent: () => import('./pos/categorias/categorias') },

      // --- Rutas EXCLUSIVAS DE ADMIN ---
      { 
        path: 'proveedores', 
        loadComponent: () => import('./pos/proveedores/proveedores'),
        data: { roles: ['ROLE_ADMIN'] } 
      },
      { 
        path: 'proveedores/nuevo', 
        loadComponent: () => import('./pos/proveedor-form/proveedor-form'),
        data: { roles: ['ROLE_ADMIN'] }
      },
      { 
        path: 'proveedores/editar/:id', 
        loadComponent: () => import('./pos/proveedor-form/proveedor-form'),
        data: { roles: ['ROLE_ADMIN'] }
      },
      // ---------------------------------------------
      { 
        path: 'pedidos', 
        loadComponent: () => import('./pos/pedidos-list/pedidos-list'),
        data: { roles: ['ROLE_ADMIN'] } 
      },
      { 
        path: 'pedidos/nuevo', 
        loadComponent: () => import('./pos/pedido-form/pedido-form'),
        data: { roles: ['ROLE_ADMIN'] } 
      },
      { 
        path: 'usuarios', 
        loadComponent: () => import('./pos/usuarios-list/usuarios-list'),
        data: { roles: ['ROLE_ADMIN'] }
      },
      { 
        path: 'usuarios/nuevo', 
        loadComponent: () => import('./layouts/admin-layout/registro/registro'),
        data: { roles: ['ROLE_ADMIN'] }
      },
      { 
        path: 'usuarios/editar/:id', 
        loadComponent: () => import('./layouts/admin-layout/registro/registro'),
        data: { roles: ['ROLE_ADMIN'] }
      },
      { 
        path: 'permisos', 
        loadComponent: () => import('./pos/permisos/permisos'),
        data: { roles: ['ROLE_ADMIN'] }
      },
      { 
        path: 'roles', 
        loadComponent: () => import('./pos/roles/roles'),
        data: { roles: ['ROLE_ADMIN'] }
      },
      { 
        path: 'reglas-puntos', 
        loadComponent: () => import('./pos/reglas-puntos/reglas-puntos'),
        data: { roles: ['ROLE_ADMIN'] } 
      },
      { 
        path: 'cotizaciones', 
        loadComponent: () => import('./pos/cotizaciones-list/cotizaciones-list'),
        data: { roles: ['ROLE_ADMIN'] } 
      },
      { 
        path: 'cotizaciones/:id', 
        loadComponent: () => import('./pos/cotizacion-detalle/cotizacion-detalle'),
        data: { roles: ['ROLE_ADMIN'] } 
      },
        
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },

  // --- MUNDO DEL CLIENTE (PORTAL) ---
  {
    path: 'portal', 
    component: PublicLayoutComponent, 
    canActivate: [AuthGuard],
    canActivateChild: [AuthGuard],
    data: {
      roles: ['ROLE_CLIENTE']
    },
    children: [
        { path: 'catalogo', loadComponent: () => import('./pages/catalogo/catalogo') }, 
        { path: 'mi-perfil', loadComponent: () => import('./pages/mi-perfil/mi-perfil') }, 
        { path: 'mis-compras', loadComponent: () => import('./pages/mis-compras/mis-compras') }, 
        {path: 'mis-puntos', loadComponent: () => import('./pages/mis-puntos/mis-puntos').then(m => m.MisPuntosComponent) },
        { path: '', redirectTo: 'catalogo', pathMatch: 'full' }
    ]
  },

  // --- RUTA PÚBLICA PARA EL PROVEEDOR ---
  {
    path: 'oferta',
    component: AuthLayoutComponent,
    children: [
      { 
        path: ':token',
        loadComponent: () => import('./pages/oferta-proveedor/oferta-proveedor')
      }
    ]
  },

  // Redirección general
  { path: '**', redirectTo: 'login' }
];