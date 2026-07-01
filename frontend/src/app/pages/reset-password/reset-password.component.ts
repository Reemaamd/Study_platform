import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.component';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.css']
})
export class ResetPasswordComponent {
  newPassword = '';
  showPassword = false;
  errorMessage = '';
  email = sessionStorage.getItem('reset-email') || '';
  code = sessionStorage.getItem('reset-code') || '';

  constructor(private authService: AuthService, private router: Router) {}

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  onSubmit(): void {
    this.errorMessage = '';
    this.authService.resetPassword(this.email, this.code, this.newPassword).subscribe({
      next: () => {
        sessionStorage.removeItem('reset-email');
        sessionStorage.removeItem('reset-code');
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.errorMessage = err?.error?.error || 'Erreur lors de la réinitialisation';
      }
    });
  }
}