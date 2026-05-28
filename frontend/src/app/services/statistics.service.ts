import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { forkJoin, Observable } from 'rxjs';

export interface AnalyseData {
  dashboard: any;
  studyTime: any;
  progress: any;
  weeklyProductivity: any[];
  subjectsStats: any[];
  dailyHours: any[];
  objectiveCompletion: any;
  currentWeek: any;
}

@Injectable({
  providedIn: 'root'
})
export class StatisticsService {

  private api = 'http://localhost:8080/statistics';

  constructor(private http: HttpClient) {}

  loadAnalyse(): Observable<AnalyseData> {

    return forkJoin({

      dashboard: this.http.get(`${this.api}/dashboard`),

      studyTime: this.http.get(`${this.api}/study-time`),

      progress: this.http.get(`${this.api}/progress`),

      weeklyProductivity: this.http.get(
        `${this.api}/weekly-productivity`
      ),

      subjectsStats: this.http.get(
        `${this.api}/subjects-stats`
      ),

      dailyHours: this.http.get(
        `${this.api}/daily-hours`
      ),

      objectiveCompletion: this.http.get(
        `${this.api}/objective-completion`
      ),

      currentWeek: this.http.get(
        `${this.api}/current-week`
      )

    }) as Observable<AnalyseData>;
  }
  private apiUrl = 'http://localhost:8080';
  private statsUrl = `${this.apiUrl}/statistics`;


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