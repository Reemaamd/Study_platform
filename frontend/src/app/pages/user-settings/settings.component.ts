import { Component, OnInit, ChangeDetectorRef, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient, HttpParams } from '@angular/common/http';
import { BottomNavComponent } from '../../components/bottom-bar/bottom-bar.component';

// ─── Models ──────────────────────────────────────────────────────────────────

export interface UserResponse {
  id: string;
  name: string;
  username: string;
  email: string;
  role: string;
}

export interface UserRequest {
  name: string;
  username: string;
  email: string;
}

// ─── Component ───────────────────────────────────────────────────────────────

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, BottomNavComponent],
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.css'],
})
export class SettingsComponent implements OnInit {

  private readonly API = 'http://localhost:8080/users';

  // ── State
  currentUser: UserResponse | null = null;
  loading = false;
  activeTab: 'profile' | 'password' | 'danger' = 'profile';
  showDeleteModal = false;

  // ── Profile form
  profileForm_data: UserRequest = { name: '', username: '', email: '' };

  // ── Password form
  passwordForm = { oldPassword: '', newPassword: '', confirmPassword: '' };
  showOld = false;
  showNew = false;
  showConfirm = false;

  // ── Toast
  toastVisible = false;
  toastError = false;
  toastMessage = '';
  private toastTimer: any;

  // ── Error state
  error = '';
  pwdError = '';
  pwdSuccess = '';
  passwordMismatch = false;

  constructor(
    private http: HttpClient,
    private router: Router,
    private cd: ChangeDetectorRef,
    private ngZone: NgZone   // ← ajout NgZone
  ) {}

  // ─── Lifecycle ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.fetchCurrentUser();
  }

  // ─── Helpers ───────────────────────────────────────────────────────────────

  checkPasswordMatch(): void {
    const confirm = this.passwordForm.confirmPassword?.trim() || '';
    const newPwd  = this.passwordForm.newPassword?.trim() || '';
    this.passwordMismatch = !!confirm && newPwd !== confirm;
  }

  getInitials(): string {
    if (!this.currentUser) return '?';
    const parts = (this.currentUser.name ?? '').trim().split(' ');
    if (parts.length >= 2) {
      return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return (parts[0]?.[0] ?? this.currentUser.username[0]).toUpperCase();
  }

  resetProfileForm(): void {
    if (!this.currentUser) return;
    this.profileForm_data = {
      name:     this.currentUser.name,
      username: this.currentUser.username,
      email:    this.currentUser.email,
    };
  }

  resetPasswordForm(): void {
    this.passwordForm    = { oldPassword: '', newPassword: '', confirmPassword: '' };
    this.passwordMismatch = false;
  }

  // ─── Toast ─────────────────────────────────────────────────────────────────

  showToast(message: string, isError = false): void {
    clearTimeout(this.toastTimer);
    this.ngZone.run(() => {           // ← ngZone.run garantit la détection
      this.toastMessage = message;
      this.toastError   = isError;
      this.toastVisible = true;
      this.cd.detectChanges();
      this.toastTimer = setTimeout(() => {
        this.toastVisible = false;
        this.cd.detectChanges();
      }, 3200);
    });
  }

  // ─── API Calls ─────────────────────────────────────────────────────────────

  /** GET /users/me */
  fetchCurrentUser(): void {
    this.loading = true;
    this.error   = '';
    this.cd.detectChanges();

    const token = localStorage.getItem('token');
    if (!token) {
      this.loading = false;
      this.router.navigate(['/login']);
      return;
    }

    this.http.get<UserResponse>(`${this.API}/me`).subscribe({
      next: (user) => {
        this.ngZone.run(() => {
          this.currentUser     = user;
          this.profileForm_data = {
            name:     user.name,
            username: user.username,
            email:    user.email,
          };
          this.loading = false;
          this.cd.detectChanges();
        });
      },
      error: (err) => {
        this.ngZone.run(() => {
          this.loading = false;
          if (err.status === 401 || err.status === 403) {
            localStorage.removeItem('token');
            sessionStorage.clear();
            this.router.navigate(['/login']);
          } else {
            this.error = `Erreur ${err.status}: ${err.error?.message || err.statusText || 'Erreur inconnue'}`;
          }
          this.cd.detectChanges();
        });
      },
    });
  }

  /** PUT /users/me */
  updateProfile(): void {
    this.loading = true;
    this.cd.detectChanges();

    this.http.put<UserResponse>(`${this.API}/me`, this.profileForm_data).subscribe({
      next: (updated) => {
        this.ngZone.run(() => {
          this.currentUser = updated;
          // Mettre à jour aussi le localStorage si tu stockes le username
          localStorage.setItem('username', updated.username);
          this.loading = false;
          this.cd.detectChanges();
          this.showToast('Profil mis à jour avec succès');
        });
      },
      error: (err) => {
        this.ngZone.run(() => {
          this.loading = false;
          this.cd.detectChanges();
          this.showToast(err.error?.message ?? 'Erreur lors de la mise à jour', true);
        });
      },
    });
  }

  /** PUT /users/change-password */
  changePassword(): void {
    this.pwdError   = '';
    this.pwdSuccess = '';

    if (!this.passwordForm.oldPassword.trim())  { this.pwdError = 'Ancien mot de passe requis'; return; }
    if (!this.passwordForm.newPassword.trim())  { this.pwdError = 'Nouveau mot de passe requis'; return; }
    if (this.passwordForm.newPassword.length < 8) { this.pwdError = 'Minimum 8 caractères.'; return; }
    if (this.passwordForm.newPassword !== this.passwordForm.confirmPassword) {
      this.pwdError = 'Mots de passe différents.';
      return;
    }
    if (!this.currentUser) return;

    this.loading = true;
    this.cd.detectChanges();

    const params = new HttpParams()
      .set('username',    this.currentUser.username)
      .set('oldPassword', this.passwordForm.oldPassword)
      .set('newPassword', this.passwordForm.newPassword);

    this.http.put(`${this.API}/change-password`, {}, { params, responseType: 'text' }).subscribe({
      next: (res) => {
        this.ngZone.run(() => {
          this.loading    = false;
          this.pwdSuccess = typeof res === 'string' ? res : 'Mot de passe mis à jour.';
          this.resetPasswordForm();
          this.cd.detectChanges();
          // Déconnexion automatique après changement de mot de passe
          // pour forcer re-login avec le nouveau mot de passe
          setTimeout(() => {
            this.pwdSuccess = '';
            this.cd.detectChanges();
          }, 4000);
        });
      },
      error: (e) => {
        this.ngZone.run(() => {
          this.loading  = false;
          this.pwdError = e.error?.message || e.error || 'Mot de passe actuel incorrect.';
          this.cd.detectChanges();
        });
      },
    });
  }

  /** DELETE /users/me */
  deleteAccount(): void {
    this.loading         = true;
    this.showDeleteModal = false;
    this.cd.detectChanges();

    this.http.delete<string>(`${this.API}/me`, { responseType: 'text' as 'json' }).subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.loading = false;
          localStorage.removeItem('token');
          sessionStorage.clear();
          this.router.navigate(['/login']);
        });
      },
      error: (err) => {
        this.ngZone.run(() => {
          this.loading = false;
          if (err.status === 401 || err.status === 403) {
            localStorage.removeItem('token');
            sessionStorage.clear();
            this.router.navigate(['/login']);
          } else {
            this.showToast(err.error ?? 'Erreur lors de la suppression du compte', true);
          }
          this.cd.detectChanges();
        });
      },
    });
  }

  // ─── Auth ──────────────────────────────────────────────────────────────────

  logout(): void {
    localStorage.removeItem('token');
    sessionStorage.clear();
    this.router.navigate(['/login'], { replaceUrl: true });
  }
}