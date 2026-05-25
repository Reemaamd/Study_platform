import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
})
export class LoginComponent {

  username     = '';
  password     = '';
  errorMessage = '';

  constructor(
    private authService: AuthService,
    private router: Router,
  ) {}

  onLogin(): void {
    this.authService.login({ username: this.username, password: this.password }).subscribe({

      next: (response: any) => {

        // ── Sauvegarder la session ─────────────────────────────────────────
        localStorage.setItem('token',    response.token);    // ✅ clé 'token'
        localStorage.setItem('role',     response.role);
        localStorage.setItem('username', response.username);

        // ── Admin → page admin ─────────────────────────────────────────────
        if (response.role === 'ADMIN') {
          this.router.navigate(['/admin']);
          return;
        }
        /*const weekKey = `onboarding_week_${response.username}`;
  const stored = localStorage.getItem(weekKey);
  //const thisMonday = this.getCurrentMondayKey();
const thisWeek = this.getCurrentWeekKeyFromLogin();
  if (stored !== thisWeek) {
    this.router.navigate(['/weekly-onboarding']);
  } else {
    this.router.navigate(['/dashboard']);
  }*/
       this.router.navigate(['/notifications']); // ← ajoute cette ligne
},

      error: (err: any) => {
        console.error(err);
        this.errorMessage = err?.error?.error || 'Erreur de connexion';
      },
    });
  }
  private getCurrentWeekKeyFromLogin(): string {
  const now = new Date();
  const monday = new Date(now);
  monday.setDate(now.getDate() - ((now.getDay() + 6) % 7));
  return monday.toISOString().slice(0, 10);
}

  private getCurrentMondayKey(): string {
    const now    = new Date();
    const monday = new Date(now);
    monday.setDate(now.getDate() - ((now.getDay() + 6) % 7));
    return monday.toISOString().slice(0, 10);
  }
}