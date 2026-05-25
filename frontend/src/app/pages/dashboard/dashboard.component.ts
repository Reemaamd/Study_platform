import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { StatisticsService } from '../../services/statistics.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {

  username = localStorage.getItem('username') || 'Utilisateur';
  dashboardData: any;
  studyTime: any;
  progress: any;
  todaySessions: any[] = [];
  weeklyProductivity: any[] = [];
  subjectsStats: any[] = [];
  dailyHours: any[] = [];
  streak: any;
  loaded = false;

  get activeCircles(): number {
    return this.dashboardData?.activeCircles ?? 0;
  }

  get streakDays(): number {
    return this.dashboardData?.streakDays ?? 14;
  }

  constructor(
    private statisticsService: StatisticsService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadAllData();
    this.statisticsService.getStreak().subscribe({
      next: (res: any) => {
        this.streak = res.streak;
        //console.log('streak:', res);
      }
      
    });
    
  }

  loadAllData() {
    forkJoin({
      dashboard: this.statisticsService.getDashboard().pipe(
        catchError((e) => {
          console.error('dashboard failed:', e?.status, e?.url);
          return of(null);
        })
      ),
      studyTime: this.statisticsService.getStudyTime().pipe(
        catchError((e) => {
          console.error('studyTime failed:', e?.status, e?.url);
          return of(null);
        })
      ),
      progress: this.statisticsService.getProgress().pipe(
        catchError((e) => {
          console.error('progress failed:', e?.status, e?.url);
          return of(null);
        })
      ),
      weekly: this.statisticsService.getWeeklyProductivity().pipe(
        catchError((e) => {
          console.error('weekly failed:', e?.status, e?.url);
          return of([]);
        })
      ),
      subjects: this.statisticsService.getSubjectsStats().pipe(
        catchError((e) => {
          console.error('subjects failed:', e?.status, e?.url);
          return of([]);
        })
      ),
      daily: this.statisticsService.getDailyHours().pipe(
        catchError((e) => {
          console.error('daily failed:', e?.status, e?.url);
          return of([]);
        })
      ),
      todaySessions: this.statisticsService.getTodaySessions().pipe(
        catchError((e) => {
          console.error('todaySessions failed:', e?.status, e?.url);
          return of([]);
        })
      )
    }).subscribe({
      next: (res) => {
        this.dashboardData = res.dashboard;
        this.studyTime = res.studyTime;
        this.progress = res.progress;
        this.weeklyProductivity = res.weekly;
        this.subjectsStats = res.subjects;
        this.dailyHours = res.daily;
        this.todaySessions = res.todaySessions || [];
        this.loaded = true;
        this.cdr.detectChanges();

        console.log('dashboardData:', this.dashboardData);
        console.log('todaySessions:', this.todaySessions);
      },
      error: (err) => {
        this.loaded = true;
        this.cdr.detectChanges();
        console.error('Dashboard error:', err);
      }
    });
  }
}