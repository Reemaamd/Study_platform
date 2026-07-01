// services/subject.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface SubjectRequest {
  name: string;
}

export interface SubjectDTO {
  id: string;
  name: string;
}

@Injectable({
  providedIn: 'root'
})
export class SubjectService {

  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/subjects`;

  createSubject(request: SubjectRequest): Observable<SubjectDTO> {
    return this.http.post<SubjectDTO>(this.baseUrl, request);
  }

  getAll(): Observable<SubjectDTO[]> {
    return this.http.get<SubjectDTO[]>(this.baseUrl);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}