import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { SidebarComponent } from '../sidebar/sidebar'; // (Se importa el componente hijo)

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, SidebarComponent], // (Se importa el componente hijo)
  templateUrl: './admin-layout.html',
  styleUrls: ['./admin-layout.css'] // (Se linkea su propio CSS)
})
export class AdminLayoutComponent {}