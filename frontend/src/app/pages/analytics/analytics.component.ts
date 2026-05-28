import {
  Component,
  OnInit,
  ChangeDetectorRef
} from '@angular/core';

import { CommonModule } from '@angular/common';

import {
  StatisticsService,
  AnalyseData
} from '../../services/statistics.service';

import {
  SubjectStatsDTO,
  WeeklyProductivityDTO
} from '../../models/statistics.models';

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './analytics.component.html',
  styleUrls: ['./analytics.component.css']
})
export class AnalyticsComponent implements OnInit {

  data: AnalyseData | null = null;

  loading = true;
  error = false;

  readonly Math = Math;

  // ── Couleurs matières ───────────────────────────────
  private readonly palette = [
    '#4caf7d',
    '#f5a623',
    '#4a9eca',
    '#e05252',
    '#9b59b6',
    '#1abc9c'
  ];

  constructor(
    private statsService: StatisticsService,
    private cdr: ChangeDetectorRef
  ) {}

  // ────────────────────────────────────────────────────
  // INIT
  // ────────────────────────────────────────────────────

  ngOnInit(): void {

    this.loading = true;

    this.statsService.loadAnalyse().subscribe({

      next: (d) => {

        console.log('DATA BACKEND = ', d);

        this.data = d;

        this.error = false;

        this.loading = false;

        this.cdr.detectChanges();
      },

      error: (err) => {

        console.log('ERREUR = ', err);

        this.error = true;

        this.loading = false;

        this.cdr.detectChanges();
      }

    });
  }

  // ────────────────────────────────────────────────────
  // KPI
  // ────────────────────────────────────────────────────

  get totalHeures(): string {

    if (!this.data?.dashboard) {
      return '0h';
    }

    return `${this.data.dashboard.totalStudyHours ?? 0}h`;
  }

  get productivite(): string {

    if (!this.data?.dashboard) {
      return '0%';
    }

    return `${Math.round(
      this.data.dashboard.completionRate ?? 0
    )}%`;
  }

  get prevuVsRealise(): string {

    if (!this.data?.studyTime) {
      return '—';
    }

    const planned =
      this.data.studyTime.plannedHours ?? 0;

    const completed =
      this.data.studyTime.completedHours ?? 0;

    if (planned === 0) {
      return '0%';
    }

    return `${Math.round(
      (completed / planned) * 100
    )}%`;
  }

  get totalSessions(): number {

    return this.data?.dashboard?.totalSessions ?? 0;
  }

  // ────────────────────────────────────────────────────
  // BAR CHART
  // ────────────────────────────────────────────────────

  get maxHeures(): number {

    if (!this.data?.dailyHours?.length) {
      return 1;
    }

    return Math.max(
      ...this.data.dailyHours.map(d => d.hours),
      1
    );
  }

  barHauteurPx(h: number): number {

    return Math.round(
      (h / this.maxHeures) * 110
    );
  }

  labelJour(day: string): string {

    const map: Record<string, string> = {

      MONDAY: 'L',
      TUESDAY: 'M',
      WEDNESDAY: 'M',
      THURSDAY: 'J',
      FRIDAY: 'V',
      SATURDAY: 'S',
      SUNDAY: 'D'
    };

    return map[day] ?? day[0];
  }

  // ────────────────────────────────────────────────────
  // SUBJECTS
  // ────────────────────────────────────────────────────

  couleurMatiere(i: number): string {

    return this.palette[
      i % this.palette.length
    ];
  }

  get totalHeuresMatières(): number {

    if (!this.data?.subjectsStats) {
      return 0;
    }

    return this.data.subjectsStats.reduce(
      (a, b) => a + b.totalHours,
      0
    );
  }

  pourcentageMatiere(
    s: SubjectStatsDTO
  ): number {

    if (this.totalHeuresMatières === 0) {
      return 0;
    }

    return Math.round(
      (s.totalHours / this.totalHeuresMatières) * 100
    );
  }

  // ────────────────────────────────────────────────────
  // WEEKLY PRODUCTIVITY
  // ────────────────────────────────────────────────────

  get sessionsRecentes():
    WeeklyProductivityDTO[] {

    return (
      this.data?.weeklyProductivity ?? []
    )
      .slice(-5)
      .reverse();
  }

 noteSession(achieved: number): string {

  if (achieved >= 4) return 'Excellent';

  if (achieved >= 3) return 'Très bon';

  if (achieved >= 2) return 'Bon';

  if (achieved >= 1) return 'Moyen';

  return 'Faible';
}

  numSemaine(w: string): string {

    const n = w.split('-W')[1];

    return n
      ? `Sem. ${n}`
      : w;
  }

  anneeSemaine(w: string): string {

    return w.split('-W')[0] ?? '';
  }

  // ────────────────────────────────────────────────────
  // DONUT
  // ────────────────────────────────────────────────────

  get tauxObjectifs(): number {

    return Math.round(
      this.data?.objectiveCompletion
        ?.completionRate ?? 0
    );
  }

  get ringDasharray(): string {

    const circ =
      2 * Math.PI * 36;

    const filled =
      (this.tauxObjectifs / 100) * circ;

    return `${filled.toFixed(1)} ${circ.toFixed(1)}`;
  }

  // ────────────────────────────────────────────────────
  // SAISON
  // ────────────────────────────────────────────────────

  get saison(): string {

    const m = new Date().getMonth();

    if (m < 3) {
      return 'SAISON 01';
    }

    if (m < 6) {
      return 'SAISON 02';
    }

    if (m < 9) {
      return 'SAISON 03';
    }

    return 'SAISON 04';
  }
}