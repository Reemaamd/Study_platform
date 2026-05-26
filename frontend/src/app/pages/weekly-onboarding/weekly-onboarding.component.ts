import { Component, OnInit, Inject, PLATFORM_ID, ChangeDetectorRef } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

export interface SubjectColor { name: string; hex: string; }

export interface ObjectiveItem {
  id: string;
  backendId?: string;
  title: string;
  priorityNum: number;
  weeklyGoal: number;
  isNew: boolean;
}

export interface SubjectItem {
  id: string;
  backendId?: string;
  name: string;
  color: SubjectColor;
  objectives: ObjectiveItem[];
  expanded: boolean;
  fromBackend: boolean;
}

export type DayKey = 'LUN' | 'MAR' | 'MER' | 'JEU' | 'VEN' | 'SAM' | 'DIM';

export interface TimeSlot {
  id: string;
  startHour: number;
  startMin: number;
  endHour: number;
  endMin: number;
}

export interface DaySlots { [day: string]: TimeSlot[]; }

@Component({
  selector: 'app-weekly-onboarding',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './weekly-onboarding.component.html',
  styleUrl: './weekly-onboarding.component.css',
})
export class WeeklyOnboardingComponent implements OnInit {

  private readonly API = 'http://localhost:8080';

  private get token(): string { return this.isBrowser ? (localStorage.getItem('token') || '') : ''; }
  get username(): string { return this.isBrowser ? (localStorage.getItem('username') || '') : ''; }
  private get headers(): HttpHeaders {
    return new HttpHeaders({ Authorization: `Bearer ${this.token}` });
  }

  loadingData    = true;
  showKeepDialog = false;
  generating     = false;
  generationDone = false;
  errorMsg       = '';

  weekIntent  = '';
  intentChips = ['Réviser examen', 'Terminer projet', 'Maintenir rythme', 'Découvrir nouveau sujet'];

  availableColors: SubjectColor[] = [
    { name: 'amber',  hex: '#D4A428' },
    { name: 'blue',   hex: '#2E5FA3' },
    { name: 'green',  hex: '#3D6B4F' },
    { name: 'purple', hex: '#7C3AED' },
    { name: 'red',    hex: '#B04060' },
    { name: 'teal',   hex: '#0D7A6B' },
  ];

  subjects: SubjectItem[] = [];
  newSubjectName = '';

  readonly days: DayKey[] = ['LUN','MAR','MER','JEU','VEN','SAM','DIM'];

  private readonly dayMap: Record<string, DayKey> = {
    LUN: 'LUN', MAR: 'MAR', MER: 'MER', JEU: 'JEU', VEN: 'VEN', SAM: 'SAM', DIM: 'DIM',
    MONDAY: 'LUN', TUESDAY: 'MAR', WEDNESDAY: 'MER',
    THURSDAY: 'JEU', FRIDAY: 'VEN', SATURDAY: 'SAM', SUNDAY: 'DIM',
    MON: 'LUN', TUE: 'MAR', WED: 'MER', THU: 'JEU', FRI: 'VEN', SAT: 'SAM', SUN: 'DIM',
  };

  private normalizeDay(raw: string): DayKey | null {
    if (!raw) return null;
    return this.dayMap[raw.toUpperCase()] ?? this.dayMap[raw] ?? null;
  }

  readonly daysFull: Record<DayKey, string> = {
    LUN: 'Lundi', MAR: 'Mardi', MER: 'Mercredi',
    JEU: 'Jeudi', VEN: 'Vendredi', SAM: 'Samedi', DIM: 'Dimanche',
  };

  daySlots: DaySlots = {};
  newSlot: Record<DayKey, { startHour: number; startMin: number; endHour: number; endMin: number }> = {} as any;

  hours   = Array.from({ length: 24 }, (_, i) => i);
  minutes = [0, 15, 30, 45];

  get totalObjectives(): number {
    return this.subjects.reduce((s, sub) => s + sub.objectives.length, 0);
  }

  get totalAvailableHours(): number {
    let mins = 0;
    for (const day of this.days) {
      for (const slot of (this.daySlots[day] || [])) {
        const start = slot.startHour * 60 + slot.startMin;
        const end   = slot.endHour   * 60 + slot.endMin;
        if (end > start) mins += end - start;
      }
    }
    return Math.round(mins / 60);
  }

  private isBrowser: boolean;

  constructor(
    private http: HttpClient,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) platformId: object,
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  async ngOnInit(): Promise<void> {
    if (!this.isBrowser) { this.loadingData = false; return; }

    if (!this.token || !this.username) {
      this.router.navigate(['/login']);
      return;
    }

    const safetyTimer = setTimeout(() => {
      this.loadingData = false;
      this.errorMsg = 'Délai dépassé — vérifiez que le backend tourne.';
      this.cdr.detectChanges();
    }, 8000);

    this.daySlots = this.buildEmptySlots();
    for (const day of this.days) {
      this.newSlot[day] = { startHour: 8, startMin: 0, endHour: 10, endMin: 0 };
    }

    try {
      await this.loadUserData();
      const weekKey = `onboarding_week_${this.username}`;
      if (localStorage.getItem(weekKey) === this.getCurrentWeekKey()) {
        this.showKeepDialog = true;
      }
    } catch (e) {
      console.error('ngOnInit loadUserData error:', e);
      this.errorMsg = 'Erreur de chargement — backend accessible ?';
    } finally {
      clearTimeout(safetyTimer);
      this.loadingData = false; // ← CORRECTION : toujours mis à false dans finally
      this.cdr.detectChanges();
    }
  }

  private async loadUserData(): Promise<void> {
    let subjectsRaw: any[] = [];
    try {
      subjectsRaw = await firstValueFrom(
        this.http.get<any[]>(`${this.API}/subjects`, { headers: this.headers })
      );
    } catch (e: any) {
      console.error('[loadUserData] GET /subjects failed:', e?.status);
      // CORRECTION : on ne relance pas l'erreur, loadingData sera mis à false dans finally
    }

    let availRaw: any[] = [];
    try {
      availRaw = await firstValueFrom(
        this.http.get<any[]>(`${this.API}/users/availabilities`, { headers: this.headers })
      );
    } catch (e: any) {
      console.error('[loadUserData] GET /availabilities failed:', e?.status);
      // CORRECTION : on ne relance pas l'erreur, loadingData sera mis à false dans finally
    }

    console.log('[loadUserData] availRaw brut:', JSON.stringify(availRaw));

    if (subjectsRaw?.length > 0) {
      this.subjects = subjectsRaw.map((s: any, idx: number) => ({
        id:          s.id,
        backendId:   s.id,
        name:        s.name,
        color:       this.availableColors[idx % this.availableColors.length],
        objectives:  [],
        expanded:    true,
        fromBackend: true,
      }));

      const today = new Date().toISOString().slice(0, 10);
      let objRaw: any[] = [];
      try {
        objRaw = await firstValueFrom(
          this.http.get<any[]>(`${this.API}/objectives/week?date=${today}`, { headers: this.headers })
        );
      } catch (e: any) {
        console.error('[loadUserData] GET /objectives/week failed:', e?.status);
        // CORRECTION : on ne relance pas l'erreur
      }

      console.log('[loadUserData] objRaw brut:', JSON.stringify(objRaw));

      const subjectObjMap: Record<string, any[]> = {};
      for (const obj of objRaw) {
        const key = obj.subjectId;
        if (!subjectObjMap[key]) subjectObjMap[key] = [];
        subjectObjMap[key].push(obj);
      }

      for (const subject of this.subjects) {
        const objs = (subjectObjMap[subject.backendId!] || [])
          .sort((a: any, b: any) => (a.priority ?? 1) - (b.priority ?? 1));

        objs.forEach((obj: any) => {
          subject.objectives.push({
            id:          obj.id,
            backendId:   obj.id,
            title:       obj.title || '',
            priorityNum: Number(obj.priority) || 1,
            weeklyGoal:  Number(obj.weeklyGoal) || 2,
            isNew:       false,
          });
        });
      }
    }

    this.daySlots = this.buildEmptySlots();
    if (Array.isArray(availRaw) && availRaw.length > 0) {
      let loaded = 0;
      availRaw.forEach((av: any, idx: number) => {
        const rawDay = av.day ?? av.Day ?? av.DAY ?? '';
        const day = this.normalizeDay(String(rawDay));

        console.log(`[avail ${idx}] rawDay="${rawDay}" → normalized="${day}"`,
          'startTime=', av.startTime, 'endTime=', av.endTime);

        if (!day) {
          console.warn(`[avail ${idx}] jour non reconnu: "${rawDay}"`);
          return;
        }

        const parseTime = (t: any): { h: number; m: number } => {
          if (typeof t === 'string') {
            const parts = t.split(':').map(Number);
            return { h: parts[0] || 0, m: parts[1] || 0 };
          }
          if (typeof t === 'object' && t !== null) {
            return { h: Number(t.hour ?? t.hours ?? 0), m: Number(t.minute ?? t.minutes ?? 0) };
          }
          return { h: 0, m: 0 };
        };

        const start = parseTime(av.startTime);
        const end   = parseTime(av.endTime);

        this.daySlots[day].push({
          id: crypto.randomUUID(),
          startHour: start.h, startMin: start.m,
          endHour:   end.h,   endMin:   end.m,
        });
        loaded++;
      });
      console.log(`[loadUserData] ${loaded}/${availRaw.length} créneaux chargés,`,
        `totalAvailableHours=${this.totalAvailableHours}`);
    } else {
      console.log('[loadUserData] Aucune disponibilité trouvée (availRaw vide ou null)');
    }

    this.cdr.detectChanges();
  }

  // ── Priority helpers ──────────────────────────────────────────────────────

  getPriorityRange(si: number): number[] {
    const count = this.subjects[si].objectives.length;
    return Array.from({ length: count }, (_, i) => i + 1);
  }

  get globalPriorityRange(): number[] {
    return Array.from(
      { length: this.totalObjectives },
      (_, i) => i + 1
    );
  }

  isPriorityTaken(
    currentSubjectIndex: number,
    currentObjectiveIndex: number,
    rank: number
  ): boolean {
    return this.subjects.some((subject, si) =>
      subject.objectives.some((obj, oi) => {
        if (si === currentSubjectIndex && oi === currentObjectiveIndex) {
          return false;
        }
        return obj.priorityNum === rank;
      })
    );
  }

  setPriorityNum(si: number, oi: number, rank: number): void {
    if (this.isPriorityTaken(si, oi, rank)) return;
    const obj = this.subjects[si].objectives[oi];
    this.subjects[si].objectives[oi] = { ...obj, priorityNum: rank };
  }

  increaseGoal(si: number, oi: number): void {
    const obj = this.subjects[si].objectives[oi];
    if (obj.weeklyGoal < 40) {
      this.subjects[si].objectives[oi] = { ...obj, weeklyGoal: obj.weeklyGoal + 1 };
    }
  }

  decreaseGoal(si: number, oi: number): void {
    const obj = this.subjects[si].objectives[oi];
    if (obj.weeklyGoal > 1) {
      this.subjects[si].objectives[oi] = { ...obj, weeklyGoal: obj.weeklyGoal - 1 };
    }
  }

  private buildEmptySlots(): DaySlots {
    const s: DaySlots = {};
    for (const d of this.days) s[d] = [];
    return s;
  }

  formatTime(h: number, m: number): string {
    return `${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}`;
  }

  slotDuration(slot: TimeSlot): string {
    const mins = (slot.endHour * 60 + slot.endMin) - (slot.startHour * 60 + slot.startMin);
    if (mins <= 0) return '—';
    const h = Math.floor(mins / 60);
    const m = mins % 60;
    return h > 0 ? (m > 0 ? `${h}h${String(m).padStart(2,'0')}` : `${h}h`) : `${m}min`;
  }

  getCurrentWeekKey(): string {
    const now    = new Date();
    const monday = new Date(now);
    monday.setDate(now.getDate() - ((now.getDay() + 6) % 7));
    return monday.toISOString().slice(0, 10);
  }

  private getWeekEndKey(): string {
    const now    = new Date();
    const sunday = new Date(now);
    sunday.setDate(now.getDate() - ((now.getDay() + 6) % 7) + 6);
    return sunday.toISOString().slice(0, 10);
  }

  keepCurrentPlan(): void { this.showKeepDialog = false; this.router.navigate(['/dashboard']); }
  startNewPlan():    void { this.showKeepDialog = false; }

  appendChip(chip: string): void {
    this.weekIntent = this.weekIntent
      ? this.weekIntent + ', ' + chip.toLowerCase()
      : chip.toLowerCase();
  }

  addSubject(): void {
    const name = this.newSubjectName.trim();
    if (!name) return;
    const color = this.availableColors[this.subjects.length % this.availableColors.length];
    this.subjects.push({
      id: crypto.randomUUID(), backendId: undefined,
      name, color, objectives: [], expanded: true, fromBackend: false,
    });
    this.newSubjectName = '';
  }

  removeSubject(idx: number): void { this.subjects.splice(idx, 1); }
  toggleSubject(idx: number): void { this.subjects[idx].expanded = !this.subjects[idx].expanded; }

  addObjective(si: number): void {
    const subject = this.subjects[si];
    const usedRanks = new Set(
      this.subjects.flatMap(s => s.objectives.map(o => o.priorityNum))
    );
    let nextRank = 1;
    while (usedRanks.has(nextRank)) nextRank++;
    this.errorMsg = '';
    subject.objectives.push({
      id: crypto.randomUUID(), title: '',
      priorityNum: nextRank, weeklyGoal: 2, isNew: true,
    });
  }

  removeObjective(si: number, oi: number): void { this.subjects[si].objectives.splice(oi, 1); }

  addSlot(day: DayKey): void {
    const ns = this.newSlot[day];
    const startMins = ns.startHour * 60 + ns.startMin;
    const endMins   = ns.endHour   * 60 + ns.endMin;
    if (endMins <= startMins) {
      this.errorMsg = "L'heure de fin doit être après l'heure de début.";
      return;
    }
    this.errorMsg = '';
    this.daySlots = {
      ...this.daySlots,
      [day]: [...this.daySlots[day], {
        id: crypto.randomUUID(),
        startHour: ns.startHour, startMin: ns.startMin,
        endHour:   ns.endHour,   endMin:   ns.endMin,
      }],
    };
    this.newSlot[day] = { startHour: 8, startMin: 0, endHour: 10, endMin: 0 };
  }

  removeSlot(day: DayKey, idx: number): void {
    this.daySlots = {
      ...this.daySlots,
      [day]: this.daySlots[day].filter((_, i) => i !== idx),
    };
  }

  hasSlotsAnyDay(): boolean {
    return this.days.some(d => this.daySlots[d].length > 0);
  }

  async generatePlan(): Promise<void> {
    if (this.subjects.length === 0) {
      this.errorMsg = 'Veuillez ajouter au moins une matière.'; return;
    }
    if (!this.hasSlotsAnyDay()) {
      this.errorMsg = 'Veuillez définir au moins une disponibilité.'; return;
    }

    this.generating = true;
    this.errorMsg   = '';

    try {
      for (const subject of this.subjects) {
        if (!subject.backendId) {
          const res: any = await firstValueFrom(
            this.http.post(`${this.API}/subjects`, { name: subject.name }, { headers: this.headers })
          );
          subject.backendId = res.id;
        }
      }

      const weekStart = this.getCurrentWeekKey();
      const weekEnd   = this.getWeekEndKey();

      for (const subject of this.subjects) {
        for (const obj of subject.objectives) {
          if (obj.isNew || !obj.backendId) {
            const res: any = await firstValueFrom(
              this.http.post(`${this.API}/objectives`, {
                subjectId:  subject.backendId,
                title:      obj.title || 'Objectif',
                weeklyGoal: obj.weeklyGoal,
                priority:   obj.priorityNum,
                weekStartDate: weekStart,
                weekEndDate:   weekEnd,
              }, { headers: this.headers })
            );
            obj.backendId = res.id;
            obj.isNew = false;
          } else {
            await firstValueFrom(
              this.http.put(`${this.API}/objectives/${obj.backendId}`, {
                subjectId:  subject.backendId,
                title:      obj.title || 'Objectif',
                weeklyGoal: obj.weeklyGoal,
                priority:   obj.priorityNum,
                weekStartDate: weekStart,
                weekEndDate:   weekEnd,
              }, { headers: this.headers })
            ).catch(e => console.error('[PUT objective] failed:', e?.status, e?.error));
          }
        }
      }

      await this.syncAvailabilities();

      await firstValueFrom(
        this.http.post(`${this.API}/study-sessions/generate`, {}, { headers: this.headers })
      );

      localStorage.setItem(`onboarding_week_${this.username}`, weekStart);
      this.generationDone = true;
      setTimeout(() => this.router.navigate(['/dashboard']), 1800);

    } catch (err: any) {
      console.error('[generatePlan] error:', err);
      this.errorMsg = err?.error?.message || err?.message || 'Erreur lors de la génération.';
    } finally {
      this.generating = false;
    }
  }

  private async syncAvailabilities(): Promise<void> {
    let currentCount = 0;
    try {
      const current = await firstValueFrom(
        this.http.get<any[]>(`${this.API}/users/availabilities`, { headers: this.headers })
      );
      currentCount = current?.length ?? 0;
    } catch { /* ignore */ }

    for (let i = currentCount - 1; i >= 0; i--) {
      await firstValueFrom(
        this.http.delete(`${this.API}/users/availabilities/${i}`, { headers: this.headers })
      ).catch(e => console.warn('[syncAvail] DELETE', i, 'failed:', e?.status));
    }

    const dayToJava: Record<DayKey, string> = {
      LUN: 'MONDAY', MAR: 'TUESDAY', MER: 'WEDNESDAY',
      JEU: 'THURSDAY', VEN: 'FRIDAY', SAM: 'SATURDAY', DIM: 'SUNDAY',
    };

    for (const day of this.days) {
      for (const slot of this.daySlots[day]) {
        await firstValueFrom(
          this.http.post(`${this.API}/users/availabilities`, {
            day:       dayToJava[day],
            startTime: this.formatTime(slot.startHour, slot.startMin),
            endTime:   this.formatTime(slot.endHour,   slot.endMin),
          }, { headers: this.headers })
        ).catch(e => console.error('[syncAvail] POST failed for', day, e?.status, e?.error));
      }
    }
  }

  trackById(_: number, item: { id: string }) { return item.id; }
}