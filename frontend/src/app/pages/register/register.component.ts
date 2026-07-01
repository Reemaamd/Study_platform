import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../services/auth.component';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent {

  name = '';
  username = '';
  email = '';
  password = '';

  successMessage = '';
  errorMessage = '';
 showPassword = false; 

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }
  
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  onRegister() {

    const data = {
      name: this.name,
      username: this.username,
      email: this.email,
      password: this.password,
      role: 'USER'
    };

    console.log(data);

    this.authService.register(data).subscribe({

      next: (response) => {

        console.log('Register response:', response);

        // After successful registration, auto-login to get JWT token
        console.log('🔄 Auto-logging in after registration...');
        
        const loginData = {
          username: this.username,
          password: this.password
        };

        this.authService.login(loginData).subscribe({
          next: (loginResponse) => {
            console.log('🔍 Login response:', loginResponse);
            console.log('🔍 Login response type:', typeof loginResponse);
            console.log('🔍 Response keys:', Object.keys(loginResponse));
            
            // Extract token from response
            let token = '';
            if (loginResponse.token) {
              token = loginResponse.token;
            } else if (typeof loginResponse === 'string' && loginResponse.includes('eyJ')) {
              token = loginResponse;
            } else if (loginResponse.access_token) {
              token = loginResponse.access_token;
            }
            
            if (!token) {
              console.error('❌ No valid JWT found in login response!');
              console.error('Full response:', loginResponse);
              this.errorMessage = 'Login successful but no JWT token received. Check backend response.';
              return;
            }
            
            console.log('✅ Login successful, token found:', token.substring(0, 30) + '...');
            
            // Save JWT token
            localStorage.setItem('token', token);
            localStorage.setItem('username', loginResponse.username);
            localStorage.setItem('role', loginResponse.role);
            
            this.router.navigate(['/onboarding']);
          },
          error: (err) => {
            console.error('❌ Auto-login failed:', err);
            this.errorMessage = 'Registration successful but login failed. Please log in manually.';
          }
        });
      },

      error: (err) => {

        console.log(err);

        this.successMessage = '';

        this.errorMessage =
          err.error || 'Erreur inscription';
      }
    });
  }
}