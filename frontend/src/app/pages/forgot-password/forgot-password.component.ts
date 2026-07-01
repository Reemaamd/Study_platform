import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.component';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.css']
})
export class ForgotPasswordComponent {
  email = '';
  errorMessage = '';

  constructor(private authService: AuthService, private router: Router) {}

  onSubmit(): void {
    this.errorMessage = '';
    this.authService.forgotPassword(this.email).subscribe({
      next: () => {
        sessionStorage.setItem('reset-email', this.email);
        this.router.navigate(['/verify-code']);
      },
      error: (err) => {
        this.errorMessage = err?.error?.error || 'Une erreur est survenue';
      }
    });
  }
}