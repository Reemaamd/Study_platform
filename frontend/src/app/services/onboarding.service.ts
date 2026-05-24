import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AvailabilityDTO {
  day: string;
  startTime: string;
  endTime: string;
}

export interface SubjectDTO {
  name: string;
  title: string;
  weeklyGoal: number;
  priority: number;
}

export interface OnboardingPayload {
  subjects: SubjectDTO[];
  availability: AvailabilityDTO[];
}

@Injectable({ providedIn: 'root' })
export class OnboardingService {

  private apiUrl = 'http://localhost:8080';

  constructor(private http: HttpClient) {}

  saveOnboarding(payload: OnboardingPayload): Observable<any> {
    console.log('📤 Onboarding payload:', payload);
    return this.http.post(`${this.apiUrl}/onboarding`, payload);
  }
}