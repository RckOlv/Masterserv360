import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-auth-layout',
  standalone: true,
  imports: [RouterModule],
  templateUrl: './auth-layout.html',
  styleUrls: ['./auth-layout.css']
})
export default class AuthLayoutComponent {
  // Este componente es solo un 'wrapper' visual
}