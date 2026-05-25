import { Injectable, inject }                        from '@angular/core';
import { HttpClient, HttpErrorResponse }              from '@angular/common/http';
import { Observable, throwError, BehaviorSubject }   from 'rxjs';
import { catchError, tap, map }                       from 'rxjs/operators';
import { environment }                                from '../../environments/environment';

// ── DTOs ──────────────────────────────────────────────────────

export type SessionStatus = 'PLANNED' | 'ONGOING' | 'DONE' | 'CANCELLED';

export interface StudySessionDTO {
  id?:         string;
  subjectId:   string;
  subjectName?: string;  // Pour afficher dans l'UI
  title?:      string;   // Ex: "Réviser chapitre 2", "TP Hadoop"
  startTime:   string;   // ISO-8601  ex : "2025-03-18T08:00:00"
  endTime:     string;
  status:      SessionStatus;
  priority?:   number;   // 1 = priorité la plus haute
}

// ── Service ───────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class StudySessionService {

  private http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/study-sessions`;

  /** Cache local — évite un re-fetch si les données n'ont pas changé */
  private _sessions$ = new BehaviorSubject<StudySessionDTO[]>([]);
  readonly sessions$  = this._sessions$.asObservable();

  // ── READ ────────────────────────────────────────────────────

  /**
   * GET /study-sessions
   * Récupère toutes les sessions de l'utilisateur connecté,
   * met à jour le cache interne et retourne l'Observable.
   */
  getAll(): Observable<StudySessionDTO[]> {
    return this.http.get<StudySessionDTO[]>(this.base).pipe(
      tap(sessions => this._sessions$.next(sessions)),
      catchError(this.handleError),
    );
  }

  // ── GENERATE ────────────────────────────────────────────────

  /**
   * POST /study-sessions/generate
   * Déclenche la génération IA du planning hebdomadaire.
   * Merge les nouvelles sessions avec les DONE existantes au lieu de tout remplacer.
   */
  generate(): Observable<StudySessionDTO[]> {
    return this.http.post<StudySessionDTO[]>(`${this.base}/generate`, null).pipe(
      tap(newSessions => {
        const current = this._sessions$.getValue();
        // Garder les sessions DONE existantes, ajouter les nouvelles
        const merged = [
          ...current.filter(s => s.status === 'DONE'),
          ...newSessions
        ];
        this._sessions$.next(merged);
      }),
      catchError(this.handleError),
    );
  }

  // ── COMPLETE ────────────────────────────────────────────────

  /**
   * PUT /study-sessions/{id}/complete
   * Marque une session comme DONE et met à jour le cache local
   * sans re-fetch complet.
   */
  complete(id: string): Observable<StudySessionDTO> {
    return this.http.put<StudySessionDTO>(`${this.base}/${id}/complete`, null).pipe(
      tap(updated => {
        const current = this._sessions$.getValue();
        const idx     = current.findIndex(s => s.id === id);
        if (idx !== -1) {
          const next  = [...current];
          next[idx]   = { ...next[idx], status: updated.status };
          this._sessions$.next(next);
        }
      }),
      catchError(this.handleError),
    );
  }

  // ── FILTER HELPER ───────────────────────────────────────────

  /**
   * Filtre les sessions pour ne garder que celles appartenant à la semaine [weekStart, weekEnd].
   * NOTE: Le backend retourne déjà les sessions de la semaine actuelle.
   * Ce helper est gardé pour usage futur (ex: filter après load).
   */
  filterByWeek(
    sessions: StudySessionDTO[],
    weekStart: Date,
    weekEnd:   Date,
  ): StudySessionDTO[] {
    return sessions.filter(s => {
      const d = new Date(s.startTime);
      return d >= weekStart && d <= weekEnd;
    });
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
    } else if (err.status === 400 && err.error?.message === 'No availability defined') {
      message = '⚠️ Aucune disponibilité définie. Veuillez configurer vos disponibilités dans les paramètres.';
    } else {
      message = `Erreur serveur (${err.status}) : ${err.error?.message ?? err.message}`;
    }

    return throwError(() => new Error(message));
  }
  
}