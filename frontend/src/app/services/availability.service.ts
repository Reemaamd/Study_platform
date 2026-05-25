import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';

// ── DTOs ──────────────────────────────────────────────────────

export interface AvailabilityDTO {
  day: string;       // ex: 'MONDAY', 'TUESDAY', etc
  startTime: string; // ex: '08:00:00'
  endTime:   string; // ex: '18:00:00'
}

export interface Availability extends AvailabilityDTO {
  id?: string;
}

// ── Service ───────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class AvailabilityService {
  private http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/users/availabilities`;

  // ── READ ────────────────────────────────────────────────────

  /**
   * GET /users/availabilities
   * Récupère les disponibilités de l'utilisateur connecté
   */
  getAll(): Observable<Availability[]> {
    return this.http.get<Availability[]>(this.base).pipe(
      catchError(this.handleError),
    );
  }

  // ── CREATE ──────────────────────────────────────────────────

  /**
   * POST /users/availabilities
   * Ajoute une nouvelle disponibilité
   */
  add(dto: AvailabilityDTO): Observable<Availability[]> {
    return this.http.post<Availability[]>(this.base, dto).pipe(
      catchError(this.handleError),
    );
  }

  // ── UPDATE ──────────────────────────────────────────────────

  /**
   * PUT /users/availabilities/{index}
   * Modifie une disponibilité existante
   */
  update(index: number, dto: AvailabilityDTO): Observable<Availability[]> {
    return this.http.put<Availability[]>(`${this.base}/${index}`, dto).pipe(
      catchError(this.handleError),
    );
  }

  // ── DELETE ──────────────────────────────────────────────────

  /**
   * DELETE /users/availabilities/{index}
   * Supprime une disponibilité
   */
  delete(index: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${index}`).pipe(
      catchError(this.handleError),
    );
  }

  // ── ERROR HANDLER ───────────────────────────────────────────

  private handleError(err: HttpErrorResponse): Observable<never> {
    let message: string;

    if (err.status === 0) {
      message = 'Impossible de joindre le serveur. Vérifiez votre connexion.';
    } else if (err.status === 401) {
      message = 'Session expirée. Veuillez vous reconnecter.';
    } else if (err.status === 403) {
      message = 'Accès refusé.';
    } else {
      message = `Erreur serveur (${err.status}) : ${err.error?.message ?? err.message}`;
    }

    return throwError(() => new Error(message));
  }
}
