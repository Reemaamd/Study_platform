import { Component, OnInit, inject, ChangeDetectorRef, ViewEncapsulation } from '@angular/core';
import { CommonModule }                                                      from '@angular/common';
import { FormsModule }                                                       from '@angular/forms';
import { StudySessionService, StudySessionDTO }                              from '../../services/study-session.service';
import { AvailabilityService, AvailabilityDTO }                              from '../../services/availability.service';
import { ObjectiveService }                                                  from '../../services/objective.service';

// ── Types ──────────────────────────────────────────────────────────────────

export type SessionStatus = 'PLANNED' | 'ONGOING' | 'DONE' | 'CANCELLED';

export interface SubjectMeta {
  name:     string;
  cssClass: string;
  dot:      string;
}

export interface WeekDay {
  label:   string;
  date:    Date;
  isToday: boolean;
}

export interface SessionBlock {
  session:   StudySessionDTO;
  subject:   SubjectMeta;
  dayIndex:  number;
  top:       number;
  height:    number;
  timeLabel: string;
  priority:  number;
  status:    SessionStatus;
}

export interface ObjectiveRequest {
  title:      string;
  subjectId:  string;
  weeklyGoal: number;
  priority:   number;
}

// ── Constants ───────────────────────────────────────────────────────────────

const HOUR_HEIGHT = 60;
const START_HOUR  = 8;
const END_HOUR    = 24;

const DAY_LABELS = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];
const MONTH_FR   = [
  'janvier', 'février', 'mars', 'avril', 'mai', 'juin',
  'juillet', 'août', 'septembre', 'octobre', 'novembre', 'décembre',
];

export const SUBJECT_PALETTE: Record<string, SubjectMeta> = {
  algebre:       { name: 'Algèbre',       cssClass: 'green',  dot: '#2d7a5a' },
  algorithmique: { name: 'Algorithmique', cssClass: 'amber',  dot: '#9a6010' },
  histoire:      { name: 'Histoire',      cssClass: 'blue',   dot: '#2a5f8a' },
  anglais:       { name: 'Anglais',       cssClass: 'pink',   dot: '#a03030' },
  lecture:       { name: 'Lecture',       cssClass: 'blue',   dot: '#2a5f8a' },
  projet:        { name: 'Projet',        cssClass: 'green',  dot: '#2d7a5a' },
  revision:      { name: 'Révision',      cssClass: 'pink',   dot: '#a03030' },
  cerclealgo:    { name: 'Cercle algo',   cssClass: 'amber',  dot: '#9a6010' },
};

// Name → palette key (from API subjectName field)
const SUBJECT_NAME_TO_KEY: Record<string, string> = {
  'Algèbre':       'algebre',
  'Algorithmique': 'algorithmique',
  'Histoire':      'histoire',
  'Anglais':       'anglais',
  'Lecture':       'lecture',
  'Projet':        'projet',
  'Révision':      'revision',
  'Cercle algo':   'cerclealgo',
};

// Fixed sidebar subjects in design order
const SIDEBAR_SUBJECTS: [string, SubjectMeta][] = [
  ['algebre',       SUBJECT_PALETTE['algebre']],
  ['algorithmique', SUBJECT_PALETTE['algorithmique']],
  ['histoire',      SUBJECT_PALETTE['histoire']],
  ['anglais',       SUBJECT_PALETTE['anglais']],
];

// Static priority labels — values must lowercase to 'haute'|'moyenne'|'basse'
const PRIORITY_LABELS: Record<string, string> = {
  algebre:       'HAUTE',
  algorithmique: 'HAUTE',
  histoire:      'MOYENNE',
  anglais:       'BASSE',
};

// Color dot lookup for fallback
const COLOR_DOTS: Record<string, string> = {
  green: '#2d7a5a',
  amber: '#9a6010',
  blue:  '#2a5f8a',
  pink:  '#a03030',
};

// ── Pure helpers ────────────────────────────────────────────────────────────

function formatWeekRange(monday: Date): string {
  const sunday = new Date(monday);
  sunday.setDate(monday.getDate() + 6);

  const options: Intl.DateTimeFormatOptions = { day: 'numeric', month: 'short' };

  const start = monday.toLocaleDateString('fr-FR', options);
  const end   = sunday.toLocaleDateString('fr-FR', options);

  return `${start} – ${end}`;
}

function mondayOf(d: Date): Date {
  const copy = new Date(d);
  const dow  = copy.getDay() || 7;
  copy.setDate(copy.getDate() - (dow - 1));
  copy.setHours(0, 0, 0, 0);
  return copy;
}

function getWeekDays(monday: Date): WeekDay[] {
  const todayStr = new Date().toDateString();
  return Array.from({ length: 7 }, (_, i) => {
    const d = new Date(monday);
    d.setDate(d.getDate() + i);
    return { label: DAY_LABELS[i], date: d, isToday: d.toDateString() === todayStr };
  });
}

function isoWeekNumber(date: Date): number {
  const tmp  = new Date(date);
  tmp.setDate(tmp.getDate() + 3);
  const jan1 = new Date(tmp.getFullYear(), 0, 1);
  return Math.ceil(((tmp.getTime() - jan1.getTime()) / 86_400_000 + jan1.getDay() + 1) / 7);
}

function fmt(d: Date): string {
  return `${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`;
}

function fallbackSubject(id: string, subjectName?: string): SubjectMeta {
  if (subjectName && SUBJECT_NAME_TO_KEY[subjectName]) {
    return SUBJECT_PALETTE[SUBJECT_NAME_TO_KEY[subjectName]];
  }
  const colors   = ['green', 'amber', 'blue', 'pink'] as const;
  const colorKey = colors[[...id].reduce((s, c) => s + c.charCodeAt(0), 0) % colors.length];
  return { name: subjectName || 'Session', cssClass: colorKey, dot: COLOR_DOTS[colorKey] };
}

function buildSessionBlocks(sessions: StudySessionDTO[], days: WeekDay[]): SessionBlock[] {
  return sessions.reduce<SessionBlock[]>((acc, s) => {
    const start = new Date(s.startTime);
    const end   = new Date(s.endTime);
    if (!s.endTime || isNaN(end.getTime())) return acc;

    const dayIndex = days.findIndex(d =>
      d.date.getFullYear() === start.getFullYear() &&
      d.date.getMonth()    === start.getMonth()    &&
      d.date.getDate()     === start.getDate()
    );
    if (dayIndex === -1) return acc;

    const startMins = start.getHours() * 60 + start.getMinutes();
    const endMins   = end.getHours()   * 60 + end.getMinutes();
    const subject   = SUBJECT_PALETTE[s.subjectId] ?? fallbackSubject(s.subjectId, s.subjectName);

    acc.push({
      session:   s,
      subject,
      dayIndex,
      top:       ((startMins - START_HOUR * 60) / 60) * HOUR_HEIGHT,
      height:    Math.max(((endMins - startMins) / 60) * HOUR_HEIGHT - 4, 28),
      timeLabel: `${fmt(start)} – ${fmt(end)}`,
      priority:  s.priority ?? 99,
      status:    s.status as SessionStatus,
    });
    return acc;
  }, []);
}

// ── Component ───────────────────────────────────────────────────────────────

@Component({
  selector:    'app-planning',
  standalone:  true,
  imports:     [CommonModule, FormsModule],
  templateUrl: './planning.component.html',
  styleUrls:   ['./planning.component.css'],
  encapsulation: ViewEncapsulation.None,
})
export class PlanningComponent implements OnInit {

  private svc          = inject(StudySessionService);
  private availSvc     = inject(AvailabilityService);
  private objectiveSvc = inject(ObjectiveService);
  private cdr          = inject(ChangeDetectorRef);

  // ── State ────────────────────────────────────────────────────────────────
  weekStart:   Date                    = mondayOf(new Date());
  days:        WeekDay[]               = [];
  blocks:      SessionBlock[]          = [];
  loading      = false;
  generating   = false;
  error:       string | null           = null;
  subjectList: [string, SubjectMeta][] = [];

  // ── Availability modal state ──────────────────────────────────────────────
  showAvailModal = false;
  availForm: AvailabilityDTO = { day: 'MONDAY', startTime: '08:00:00', endTime: '18:00:00' };
  availLoading   = false;
  availError:    string | null = null;

  // ── Objective modal state ─────────────────────────────────────────────────
  showObjectiveModal = false;
  objectiveForm: ObjectiveRequest = {
    title:      '',
    subjectId:  '',
    weeklyGoal: 5,
    priority:   1,  // ← valeur par défaut, doit être un nombre
  };
  objectiveLoading = false;
  objectiveError:  string | null = null;

  // ── Template constants ───────────────────────────────────────────────────
  readonly hours          = Array.from({ length: END_HOUR - START_HOUR }, (_, i) => START_HOUR + i);
  readonly dayColumns     = Array.from({ length: 7 });
  readonly sidebarSubjects: [string, SubjectMeta][] = SIDEBAR_SUBJECTS;

  // ── Computed ─────────────────────────────────────────────────────────────
  get weekNumber(): number { return isoWeekNumber(this.weekStart); }
  get weekLabel():  string { return formatWeekRange(this.weekStart); }

  blocksForDay(dayIndex: number): SessionBlock[] {
    return this.blocks.filter(b => b.dayIndex === dayIndex);
  }

  priorityFor(subjectId: string): string {
    if (PRIORITY_LABELS[subjectId]) return PRIORITY_LABELS[subjectId];
    const vals = this.blocks
      .filter(b => b.session.subjectId === subjectId && b.priority !== 99)
      .map(b => b.priority);
    return vals.length ? String(Math.min(...vals)) : '—';
  }

  blockStyle(b: SessionBlock): Record<string, string> {
    return { top: `${b.top}px`, height: `${b.height}px` };
  }

  blockClasses(b: SessionBlock): Record<string, boolean> {
    return {
      'session-block':      true,
      [b.subject.cssClass]: true,
      'completed':          b.status === 'DONE' || b.status === 'CANCELLED',
      'ongoing':            b.status === 'ONGOING',
    };
  }

  private buildSubjectList(sessions: StudySessionDTO[]): void {
    const seen = new Set<string>();
    const list: [string, SubjectMeta][] = [];

    for (const session of sessions) {
      if (!seen.has(session.subjectId)) {
        seen.add(session.subjectId);
        const subject = SUBJECT_PALETTE[session.subjectId] ??
                        fallbackSubject(session.subjectId, session.subjectName);
        list.push([session.subjectId, subject]);
      }
    }

    this.subjectList = list;
  }

  // ── Lifecycle ─────────────────────────────────────────────────────────────
  ngOnInit(): void { this.load(); }

  // ── Actions ───────────────────────────────────────────────────────────────
  load(): void {
    this.loading = true;
    this.error   = null;
    this.days    = getWeekDays(this.weekStart);

    const weekEnd = new Date(this.days[6].date);
    weekEnd.setHours(23, 59, 59, 999);

    this.svc.getAll().subscribe({
      next: all => {
        const week = all.filter(s => {
          const d = new Date(s.startTime);
          return d >= this.weekStart && d <= weekEnd;
        });
        this.blocks  = buildSessionBlocks(week, this.days);
        this.buildSubjectList(all);
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: err => {
        this.error   = err.message ?? 'Erreur de chargement';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  changeWeek(delta: number): void {
    this.weekStart = new Date(this.weekStart);
    this.weekStart.setDate(this.weekStart.getDate() + delta * 7);
    this.load();
  }

  generate(): void {
    this.generating = true;
    this.error      = null;

    this.svc.generate().subscribe({
      next: sessions => {
        this.days       = getWeekDays(this.weekStart);
        this.blocks     = buildSessionBlocks(sessions, this.days);
        this.buildSubjectList(sessions);
        this.generating = false;
        this.cdr.markForCheck();
      },
      error: err => {
        this.error      = err.message ?? 'Échec de la génération';
        this.generating = false;
        this.cdr.markForCheck();
      },
    });
  }

  complete(b: SessionBlock, event: Event): void {
    event.stopPropagation();
    if (!b.session.id || b.status === 'DONE' || b.status === 'CANCELLED') return;

    this.svc.complete(b.session.id).subscribe({
      next: updated => {
        b.status = updated.status as SessionStatus;
        this.cdr.markForCheck();
      },
      error: err => {
        this.error = err.message;
        this.cdr.markForCheck();
      },
    });
  }

  // ── Availability modal ────────────────────────────────────────────────────

  openAvailModal(): void {
    this.showAvailModal = true;
    this.availError     = null;
    this.availForm      = { day: 'MONDAY', startTime: '08:00:00', endTime: '18:00:00' };
  }

  closeAvailModal(): void {
    this.showAvailModal = false;
    this.availError     = null;
  }

  addAvailability(): void {
    if (!this.availForm.day || !this.availForm.startTime || !this.availForm.endTime) {
      this.availError = 'Tous les champs sont obligatoires.';
      return;
    }

    this.availLoading = true;
    this.availError   = null;

    const pad = (t: string) => t.split(':').length === 2 ? `${t}:00` : t;

    const dto: AvailabilityDTO = {
      day:       this.availForm.day,
      startTime: pad(this.availForm.startTime),
      endTime:   pad(this.availForm.endTime),
    };

    this.availSvc.add(dto).subscribe({
      next: () => {
        this.availLoading = false;
        this.closeAvailModal();
        this.load();
        this.cdr.markForCheck();
      },
      error: err => {
        this.availError   = err.message ?? "Erreur lors de l'ajout";
        this.availLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  // ── Objective modal ───────────────────────────────────────────────────────

  openObjectiveModal(): void {
    this.showObjectiveModal = true;
    this.objectiveError     = null;
    this.objectiveForm      = {
      title:      '',
      subjectId:  this.subjectList[0]?.[0] ?? '',
      weeklyGoal: 5,
      priority:   1,
    };
  }

  closeObjectiveModal(): void {
    this.showObjectiveModal = false;
    this.objectiveError     = null;
  }

  addObjective(): void {
    if (!this.objectiveForm.title.trim()) {
      this.objectiveError = 'Le titre est obligatoire.';
      return;
    }
    if (!this.objectiveForm.subjectId) {
      this.objectiveError = 'Veuillez sélectionner une matière.';
      return;
    }
    if (!this.objectiveForm.weeklyGoal || this.objectiveForm.weeklyGoal < 1) {
      this.objectiveError = "L'objectif hebdomadaire doit être ≥ 1h.";
      return;
    }

    this.objectiveLoading = true;
    this.objectiveError   = null;

    this.objectiveSvc.create(this.objectiveForm).subscribe({
      next: () => {
        this.objectiveLoading = false;
        this.closeObjectiveModal();
        this.cdr.markForCheck();
      },
      error: err => {
        this.objectiveError   = err.message ?? "Erreur lors de la création";
        this.objectiveLoading = false;
        this.cdr.markForCheck();
      },
    });
    console.log('📤 Payload envoyé:', JSON.stringify(this.objectiveForm));  // ← ajoute ça
  }
}