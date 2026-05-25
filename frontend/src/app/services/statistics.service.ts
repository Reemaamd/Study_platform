import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class StatisticsService {

  private apiUrl = 'http://localhost:8080';
  private statsUrl = `${this.apiUrl}/statistics`;

  constructor(private http: HttpClient) {}

  // ---------------- USER DASHBOARD ----------------

  getDashboard(): Observable<any> {
    return this.http.get<any>(`${this.statsUrl}/dashboard`);
  }

  getStudyTime(): Observable<any> {
    return this.http.get<any>(`${this.statsUrl}/study-time`);
  }

  getProgress(): Observable<any> {
    return this.http.get<any>(`${this.statsUrl}/progress`);
  }

  getWeeklyProductivity(): Observable<any[]> {
    return this.http.get<any[]>(`${this.statsUrl}/weekly-productivity`);
  }

  getSubjectsStats(): Observable<any[]> {
    return this.http.get<any[]>(`${this.statsUrl}/subjects-stats`);
  }

  getDailyHours(): Observable<any[]> {
    return this.http.get<any[]>(`${this.statsUrl}/daily-hours`);
  }

  // Fetch today's sessions for the current user
getTodaySessions(): Observable<any[]> {
  return this.http.get<any[]>(`${this.statsUrl}/today-sessions`);
}

  getObjectiveCompletion(): Observable<any> {
    return this.http.get<any>(`${this.statsUrl}/objective-completion`);
  }

  getCurrentWeek(): Observable<any> {
    return this.http.get<any>(`${this.statsUrl}/current-week`);
  }

  // ---------------- ADMIN ----------------

  getAdminDashboard(): Observable<any> {
    return this.http.get(`${this.apiUrl}/admin/dashboard`);
  }

  getUsersStats(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/admin/users-stats`);
  }

  getAdminSubjectsStats(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/admin/subjects-stats`);
  }

  getWeeklyTrend(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/admin/weekly-trend`);
  }

  getStreak(): Observable<any[]>{
  return this.http.get<any>(`${this.statsUrl}/streak`);
}

}