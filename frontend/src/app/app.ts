import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

// ⚠️ NE PAS injecter WeekGuardService ici.
// La vérification de semaine se fait dans login.component.ts
// après authentification réussie. Si on le met ici,
// il se déclenche AVANT le login et redirige en boucle.

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: `<router-outlet></router-outlet>`
})
export class AppComponent {}