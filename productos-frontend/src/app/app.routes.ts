import { Routes } from '@angular/router';
import { AuthGuard } from './guards/auth.guard';
import { LoginGuard } from './guards/login.guard';

// Importamos los layouts
import { AdminLayoutComponent } from './layouts/admin-layout/admin-layout';
import { PublicLayoutComponent } from './layouts/public-layout/public-layout';
import { AuthLayoutComponent } from './layouts/auth-layout/auth-layout'; 

export const routes: Routes = [

  // 1. MUNDO PÚBLICO / CLIENTE
  {
    path: '',
    component: PublicLayoutComponent,
    children: [
      { path: '', redirectTo: 'catalogo', pathMatch: 'full' },
      { path: 'catalogo', loadComponent: () => import('./pages/catalogo/catalogo') },
      
      { 
        path: 'portal',
        canActivate: [AuthGuard], 
        children: [
           { path: 'mi-perfil', loadComponent: () => import('./pages/mi-perfil/mi-perfil') },
           { path: 'mis-compras', loadComponent: () => import('./pages/mis-compras/mis-compras') },
           { path: 'mis-puntos', loadComponent: () => import('./pages/mis-puntos/mis-puntos').then(m => m.MisPuntosComponent) }
        ]
      }
    ]
  },

  // 2. AUTH (Login, Registro y Recuperación)
  {
    path: 'auth',
    component: AuthLayoutComponent, 
    children: [
      { path: 'login', loadComponent: () => import('./pages/login/login'), canActivate: [LoginGuard] },
      { path: 'register', loadComponent: () => import('./pages/reg-cli/reg-cli'), canActivate: [LoginGuard] },
      
      // --- NUEVAS RUTAS DE RECUPERACIÓN ---
      { 
        path: 'forgot-password', 
        loadComponent: () => import('./pages/auth/forgot-password/forgot-password').then(m => m.ForgotPasswordComponent) 
      },
      { 
        path: 'reset-password', 
        loadComponent: () => import('./pages/auth/reset-password/reset-password').then(m => m.ResetPasswordComponent) 
      },
      // ------------------------------------

      { path: 'cambiar-password-force', loadComponent: () => import('./pages/change-password-force/change-password-force') }
    ]
  },

  // 3. RUTAS EXTERNAS (PROVEEDORES)
  {
    path: 'oferta',
    component: AuthLayoutComponent, 
    children: [
      { path: ':token', loadComponent: () => import('./pages/oferta-proveedor/oferta-proveedor') }
    ]
  },
  
  {
    path: 'proveedor/pedido',
    component: AuthLayoutComponent, 
    children: [
      { path: ':token', loadComponent: () => import('./pages/pedido-proveedor/pedido-proveedor').then(m => m.PedidoProveedorComponent) }
    ]
  },

  // Redirecciones (Atajos)
  { path: 'login', redirectTo: 'auth/login' },
  { path: 'register', redirectTo: 'auth/register' },
  // Atajos para las nuevas rutas
  { path: 'forgot-password', redirectTo: 'auth/forgot-password' },
  { path: 'reset-password', redirectTo: 'auth/reset-password' },

  // 4. MUNDO ADMIN (POS)
  {
    path: 'pos',
    component: AdminLayoutComponent,
    canActivate: [AuthGuard], 
    data: { roles: ['ROLE_ADMIN', 'ROLE_VENDEDOR'] }, 
    children: [
      { path: 'dashboard', loadComponent: () => import('./pos/dashboard/dashboard') },
      { path: 'punto-venta', loadComponent: () => import('./pos/punto-venta/punto-venta') },
      { path: 'ventas-historial', loadComponent: () => import('./pos/ventas-list/ventas-list') },
      { path: 'ventas/:id', loadComponent: () => import('./pos/venta-detalle/venta-detalle') },
      { path: 'productos', loadComponent: () => import('./pos/productos/productos') },
      { path: 'productos/nuevo', loadComponent: () => import('./pos/producto-form/producto-form') },
      { path: 'productos/editar/:id', loadComponent: () => import('./pos/producto-form/producto-form') },
      { path: 'categorias', loadComponent: () => import('./pos/categorias/categorias') },
      
      // Admin only
      { path: 'usuarios', loadComponent: () => import('./pos/usuarios-list/usuarios-list'), data: { roles: ['ROLE_ADMIN'] } },
      { path: 'usuarios/nuevo', loadComponent: () => import('./layouts/admin-layout/registro/registro') },
      { path: 'usuarios/editar/:id', loadComponent: () => import('./layouts/admin-layout/registro/registro') },
      
      { path: 'proveedores', loadComponent: () => import('./pos/proveedores/proveedores') },
      { path: 'proveedores/nuevo', loadComponent: () => import('./pos/proveedor-form/proveedor-form') },
      { path: 'proveedores/editar/:id', loadComponent: () => import('./pos/proveedor-form/proveedor-form') },
      
      { path: 'pedidos', loadComponent: () => import('./pos/pedidos-list/pedidos-list') },
      { path: 'pedidos/nuevo', loadComponent: () => import('./pos/pedido-form/pedido-form') },
      
      { path: 'cotizaciones', loadComponent: () => import('./pos/cotizaciones-list/cotizaciones-list') },
      { path: 'cotizaciones/:id', loadComponent: () => import('./pos/cotizacion-detalle/cotizacion-detalle') },
      
      { path: 'solicitudes', loadComponent: () => import('./pos/solicitudes-list/solicitudes-list') },
      { path: 'permisos', loadComponent: () => import('./pos/permisos/permisos') },
      { path: 'roles', loadComponent: () => import('./pos/roles/roles') },
      { path: 'reglas-puntos', loadComponent: () => import('./pos/reglas-puntos/reglas-puntos') },
      { path: 'auditoria', loadComponent: () => import('./pos/auditoria/auditoria') },
      { path: 'mi-perfil', loadComponent: () => import('./pos/perfil-usuario/perfil-usuario') },
      
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },

  { path: '**', redirectTo: '' }
];