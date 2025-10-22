import { Routes } from '@angular/router';
import { AdminLayoutComponent } from './layouts/admin-layout/admin-layout';
import { PublicLayoutComponent } from './layouts/public-layout/public-layout';
import { LoginComponent } from './pages/login/login';
import { RegistroAdminComponent } from './layouts/admin-layout/registro/registro';
import { DashboardComponent } from './pos/dashboard/dashboard';
import { ProductosComponent } from './pos/productos/productos';
import { CategoriasComponent } from './pos/categorias/categorias';
import { UsuarioListComponent } from './pos/usuarios-list/usuarios-list';
import { AuthGuard } from './service/auth.guard';

export const routes: Routes = [
  // 🔹 Páginas públicas
  {
    path: '',
    component: PublicLayoutComponent,
    children: [
      { path: '', redirectTo: 'login', pathMatch: 'full' },
      { path: 'login', component: LoginComponent },
      { path: 'registro', component: RegistroAdminComponent },
      // 🔹 Página principal para clientes logueados
    { path: 'home', loadComponent: () => import('./pages/reg-cli/reg-cli').then(m => m.RegistroClienteComponent) }
    ],
  },

  // 🔹 Páginas del POS (solo autenticados)
  {
    path: 'pos',
    component: AdminLayoutComponent,
    canActivate: [AuthGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: DashboardComponent },
      { path: 'productos', component: ProductosComponent },
      { path: 'categorias', component: CategoriasComponent },
      { path: 'usuarios', component: UsuarioListComponent },
    ],
  },

  // 🔹 Ruta fallback
  { path: '**', redirectTo: '' },
];
