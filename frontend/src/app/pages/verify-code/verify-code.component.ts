import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.component';

@Component({
  selector: 'app-verify-code',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './verify-code.component.html',
  styleUrls: ['./verify-code.component.css']
})
export class VerifyCodeComponent {
  code = '';
  errorMessage = '';
  email = sessionStorage.getItem('reset-email') || '';

  constructor(private authService: AuthService, private router: Router) {}

  onSubmit(): void {
    this.errorMessage = '';
    this.authService.verifyCode(this.email, this.code).subscribe({
      next: () => {
        sessionStorage.setItem('reset-code', this.code);
        this.router.navigate(['/reset-password']);
      },
      error: (err) => {
        this.errorMessage = err?.error?.error || 'Code invalide ou expiré';
      }
    });
  }
}