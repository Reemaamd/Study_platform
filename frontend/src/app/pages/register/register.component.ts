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
  role = '';

  successMessage = '';
  errorMessage = '';

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
      role: this.role
    };

    console.log(data);

    this.authService.register(data).subscribe({

      next: (response) => {

        console.log(response);

        this.successMessage =
          'Compte créé avec succès';

        this.errorMessage = '';

        // redirection après 2 sec
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 2000);
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