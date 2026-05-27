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

  getStudyTime(startDate?: string, endDate?: string): Observable<any> {
    let url = `${this.statsUrl}/study-time`;
    if (startDate && endDate) {
      url += `?startDate=${startDate}&endDate=${endDate}`;
    }
    return this.http.get<any>(url);
  }

  getProgress(): Observable<any> {
    return this.http.get<any>(`${this.statsUrl}/progress`);
  }

  getWeeklyProductivity(startDate?: string, endDate?: string): Observable<any[]> {
    let url = `${this.statsUrl}/weekly-productivity`;
    if (startDate && endDate) {
      url += `?startDate=${startDate}&endDate=${endDate}`;
    }
    return this.http.get<any[]>(url);
  }

  getSubjectsStats(startDate?: string, endDate?: string): Observable<any[]> {
    let url = `${this.statsUrl}/subjects-stats`;
    if (startDate && endDate) {
      url += `?startDate=${startDate}&endDate=${endDate}`;
    }
    return this.http.get<any[]>(url);
  }

  getDailyHours(startDate?: string, endDate?: string): Observable<any[]> {
    let url = `${this.statsUrl}/daily-hours`;
    if (startDate && endDate) {
      url += `?startDate=${startDate}&endDate=${endDate}`;
    }
    return this.http.get<any[]>(url);
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

  getStreak(): Observable<any> {
    return this.http.get<any>(`${this.statsUrl}/streak`);
  }

}