import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface SubjectDTO {
  _id?: string;
  id?: string;
  name: string;
  userId: string;
}

@Injectable({ providedIn: 'root' })
export class SubjectService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/subjects`;

  /**
   * GET /subjects
   * Récupère tous les subjects de l'utilisateur connecté
   */
  getAll(): Observable<SubjectDTO[]> {
    return this.http.get<SubjectDTO[]>(this.baseUrl);
  }
}
