import { ChangeDetectorRef, Component, NgZone, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { StatisticsService } from '../../services/statistics.service';
import { BottomNavComponent } from '../../components/bottom-bar/bottom-bar.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, BottomNavComponent],
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
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone
  ) {}

  ngOnInit(): void {
    this.loadAllData();
  }

  private getWeekRange(): { start: Date; end: Date } {
    const today = new Date();
    const day = today.getDay();
    const diffToMonday = day === 0 ? -6 : 1 - day;

    const start = new Date(today); // ✅ copie indépendante
    start.setDate(today.getDate() + diffToMonday);
    start.setHours(0, 0, 0, 0);

    const end = new Date(start); // ✅ copie depuis start
    end.setDate(start.getDate() + 6);
    end.setHours(23, 59, 59, 999);

    return { start, end };
  }

  private formatDateForAPI(date: Date): string {
    return date.toISOString().split('T')[0]; // Returns YYYY-MM-DD
  }

  private isDateInCurrentWeek(date: string | Date): boolean {
    const { start, end } = this.getWeekRange();
    const d = typeof date === 'string' ? new Date(date) : date;
    return d >= start && d <= end;
  }

  loadAllData() {
    const { start, end } = this.getWeekRange();
    const startDate = this.formatDateForAPI(start);
    const endDate = this.formatDateForAPI(end);

    forkJoin({
      dashboard: this.statisticsService.getDashboard().pipe(
        catchError((e) => {
          console.error('dashboard failed:', e?.status, e?.url);
          return of(null);
        })
      ),
      studyTime: this.statisticsService.getStudyTime(startDate, endDate).pipe(
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
      weekly: this.statisticsService.getWeeklyProductivity(startDate, endDate).pipe(
        catchError((e) => {
          console.error('weekly failed:', e?.status, e?.url);
          return of([]);
        })
      ),
      subjects: this.statisticsService.getSubjectsStats(startDate, endDate).pipe(
        catchError((e) => {
          console.error('subjects failed:', e?.status, e?.url);
          return of([]);
        })
      ),
      daily: this.statisticsService.getDailyHours(startDate, endDate).pipe(
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
      ),
      streak: this.statisticsService.getStreak().pipe(
        catchError((e) => {
          console.error('streak failed:', e?.status, e?.url);
          return of({ streak: 0 });
        })
      )
    }).subscribe({
      next: (res) => {
        this.ngZone.run(() => {
          this.dashboardData = res.dashboard;
          this.studyTime = res.studyTime;
          this.progress = res.progress;
          this.weeklyProductivity = res.weekly ?? [];
          this.subjectsStats = res.subjects ?? [];
          this.dailyHours = res.daily ?? [];
          this.todaySessions = res.todaySessions ?? [];
          this.streak = res.streak?.streak ?? 0;
          this.loaded = true;
          this.cdr.detectChanges();

          console.log('dashboardData:', this.dashboardData);
          console.log('subjectsStats:', this.subjectsStats);
          console.log('dailyHours:', this.dailyHours);
          console.log('todaySessions:', this.todaySessions);
          console.log('streak:', this.streak);
        });
      },
      error: (err) => {
        this.ngZone.run(() => {
          this.loaded = true;
          this.cdr.detectChanges();
          console.error('Dashboard error:', err);
        });
      }
    });
  }
}