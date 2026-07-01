import { Component } from '@angular/core';

import { Router, RouterOutlet } from '@angular/router';

import { CommonModule } from '@angular/common';

import { BottomNavComponent } from './shared/bottom-nav/bottom-nav.component';

// ⚠️ NE PAS injecter WeekGuardService ici.
// La vérification de semaine se fait dans login.component.ts
// après authentification réussie. Si on le met ici,
// il se déclenche AVANT le login et redirige en boucle.

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    BottomNavComponent
  ],
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class AppComponent {

  constructor(
    private router: Router
  ) {}

  showBottomNavbar(): boolean {

    const token =
      localStorage.getItem('token');

    const role =
      localStorage.getItem('role');

    const currentUrl =
      this.router.url;

    const hiddenRoutes = [

      '/',
      '/login',
      '/register',
      '/onboarding'
    ];

    return !!token
      && role === 'USER'
      && !hiddenRoutes.includes(currentUrl);
  }

}