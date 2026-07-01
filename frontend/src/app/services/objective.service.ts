// services/objective.service.ts — version finale unique
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ObjectiveRequest {
  title: string;
  subjectId: string;
  weeklyGoal: number;
  priority: number;
}

export interface ObjectiveDTO {
  id: string;
  subjectId: string;
  subjectName?: string;
  title: string;
  weeklyGoal: number;
  priority: number;
  progress?: number;
  progressPercentage?: number;
  status?: string;
  weekStartDate?: string;
  weekEndDate?: string;
}

@Injectable({ providedIn: 'root' })
export class ObjectiveService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/objectives`;

  create(request: ObjectiveRequest): Observable<ObjectiveDTO> {
    return this.http.post<ObjectiveDTO>(this.baseUrl, request);
  }

  getAll(): Observable<ObjectiveDTO[]> {
    return this.http.get<ObjectiveDTO[]>(this.baseUrl);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}