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
}