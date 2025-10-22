import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { CommonModule,NgIf, NgFor } from '@angular/common';

@Component({
  selector: 'app-sidebar',
  standalone: true,  // 👈 Importante
  imports: [RouterModule,NgIf,NgFor], // 👈 Esto habilita routerLink, routerLinkActive, router-outlet
  templateUrl: './sidebar.html',
  styleUrls: ['./sidebar.css']
})
export class SidebarComponent {
  sidebarToggled = false;

  toggleSidebar() {
    this.sidebarToggled = !this.sidebarToggled;
  }
}
