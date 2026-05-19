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
  styleUrls: ['./login.component.css']
})
export class LoginComponent {

  username = '';
  password = '';

  errorMessage = '';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  onLogin() {

    const data = {
      username: this.username,
      password: this.password
    };

    this.authService.login(data).subscribe({

      next: (response) => {

        console.log(response);

        // sauvegarder JWT
        localStorage.setItem('token', response.token);

        // sauvegarder infos utilisateur
        localStorage.setItem('role', response.role);
        localStorage.setItem('username', response.username);

        // redirection
        this.router.navigate(['/']);
      },

      error: (err) => {

        console.log(err);

        this.errorMessage =
          err.error.error || 'Erreur de connexion';
      }
    });
  }
}