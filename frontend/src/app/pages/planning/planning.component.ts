import { Component, OnInit, inject, ChangeDetectorRef, ViewEncapsulation } from '@angular/core';
import { CommonModule }                                                      from '@angular/common';
import { FormsModule }                                                       from '@angular/forms';
import { BottomNavComponent }                                                 from '../../components/bottom-bar/bottom-bar.component';
import { forkJoin, of }                                                      from 'rxjs';
import { catchError }                                                        from 'rxjs/operators';
import { StudySessionService, StudySessionDTO }                              from '../../services/study-session.service';
import { AvailabilityService, AvailabilityDTO }                              from '../../services/availability.service';
import { ObjectiveService }                                                  from '../../services/objective.service';
import { SubjectService, SubjectDTO }                                         from '../../services/subject.service';

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
    d.setHours(0, 0, 0, 0);  // ← Forcer les heures à minuit
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

function fallbackSubject(id: string | undefined, subjectName?: string): SubjectMeta {
  if (subjectName && SUBJECT_NAME_TO_KEY[subjectName]) {
    return SUBJECT_PALETTE[SUBJECT_NAME_TO_KEY[subjectName]];
  }
  const colors   = ['green', 'amber', 'blue', 'pink'] as const;
  const idStr    = String(id || '');
  const colorKey = colors[Array.from(idStr).reduce((s, c) => s + c.charCodeAt(0), 0) % colors.length];
  return { name: subjectName || 'Session', cssClass: colorKey, dot: COLOR_DOTS[colorKey] };
}

function buildSessionBlocks(sessions: StudySessionDTO[], days: WeekDay[]): SessionBlock[] {
  console.log('🔧 buildSessionBlocks - Input:');
  console.log('   Sessions count:', sessions.length);
  console.log('   Sessions:', sessions);
  console.log('   Available days:', days.map(d => d.date.toDateString()));
  
  const blocks = sessions.reduce<SessionBlock[]>((acc, s) => {
    const start = new Date(s.startTime);
    const end   = new Date(s.endTime);
    if (!s.endTime || isNaN(end.getTime())) {
      console.log(`❌ Invalid end date for session ${s.id}`);
      return acc;
    }

    // Valider que la durée est logique (endTime > startTime)
    if (end.getTime() <= start.getTime()) {
      console.log(`❌ Session ${s.id}: endTime (${end.toISOString()}) <= startTime (${start.toISOString()})`);
      return acc;
    }

    // Créer une date locale à minuit depuis la chaîne ISO pour éviter les décalages de fuseau horaire
    const dateStr = s.startTime.split('T')[0]; // "2026-05-17"
    const [year, month, date] = dateStr.split('-').map(Number);
    const sessionDateLocal = new Date(year, month - 1, date, 0, 0, 0, 0);
    
    console.log(`📅 Session ${s.id}: startTime=${s.startTime}, parsed to ${sessionDateLocal.toDateString()}`);
    
    // Trouver l'index du jour
    const dayIndex = days.findIndex(d =>
      d.date.getFullYear() === sessionDateLocal.getFullYear() &&
      d.date.getMonth()    === sessionDateLocal.getMonth()    &&
      d.date.getDate()     === sessionDateLocal.getDate()
    );
    
    if (dayIndex === -1) {
      console.log(`❌ Session ${s.id} (${sessionDateLocal.toDateString()}) not matched to any day`);
      return acc;
    }

    console.log(`✅ Session ${s.id} matched to day index ${dayIndex}`);

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
  
  console.log('📊 Final blocks count:', blocks.length, blocks);
  return blocks;
}

// ── Component ───────────────────────────────────────────────────────────────

@Component({
  selector:    'app-planning',
  standalone:  true,
  imports:     [CommonModule, FormsModule, BottomNavComponent],
  templateUrl: './planning.component.html',
  styleUrls:   ['./planning.component.css'],
  encapsulation: ViewEncapsulation.None,
})
export class PlanningComponent implements OnInit {

  private svc          = inject(StudySessionService);
  private availSvc     = inject(AvailabilityService);
  private objectiveSvc = inject(ObjectiveService);
  private subjectSvc   = inject(SubjectService);
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

  private buildSubjectList(sessions: StudySessionDTO[], objectives?: any[], allSubjects?: SubjectDTO[]): void {
    const seen = new Set<string>();
    const list: [string, SubjectMeta][] = [];

    // Assurer que objectives et allSubjects sont des arrays
    const objectiveList = Array.isArray(objectives) ? objectives : [];
    const subjectsList = Array.isArray(allSubjects) ? allSubjects : [];

    console.log('buildSubjectList called with:');
    console.log('  Sessions:', sessions.length);
    console.log('  Objectives:', objectiveList.length);
    console.log('  All Subjects:', subjectsList.length, subjectsList);

    // 1. Ajouter TOUS les subjects de l'utilisateur d'abord (source de vérité)
    for (const subj of subjectsList) {
      const subjectId = subj?._id || subj?.id;
      const subjectName = subj?.name;
      
      const idStr = String(subjectId || '').trim();
      
      if (idStr && !seen.has(idStr)) {
        console.log(`Adding all-subjects entry: ${idStr} (${subjectName})`);
        seen.add(idStr);
        const subject = SUBJECT_PALETTE[idStr] ??
                        fallbackSubject(idStr, subjectName);
        list.push([idStr, subject]);
      }
    }

    // 2. Ajouter les matières des objectifs (pour les cas où elles ne seraient pas dans allSubjects)
    for (const obj of objectiveList) {
      const subjectId = obj?.subjectId || obj?._id || obj?.subject?.id || obj?.subject?._id;
      const subjectName = obj?.subjectName || obj?.name || obj?.subject?.name;
      
      const idStr = String(subjectId || '').trim();
      
      if (idStr && !seen.has(idStr)) {
        console.log(`Adding objective subject: ${idStr} (${subjectName})`);
        seen.add(idStr);
        const subject = SUBJECT_PALETTE[idStr] ??
                        fallbackSubject(idStr, subjectName);
        list.push([idStr, subject]);
      }
    }

    // 3. Ajouter les matières des sessions (pour les cas où elles ne seraient pas dans allSubjects)
    for (const session of sessions) {
      if (session?.subjectId && !seen.has(session.subjectId)) {
        console.log(`Adding session subject: ${session.subjectId} (${session.subjectName})`);
        seen.add(session.subjectId);
        const subject = SUBJECT_PALETTE[session.subjectId] ??
                        fallbackSubject(session.subjectId, session.subjectName);
        list.push([session.subjectId, subject]);
      }
    }

    console.log('Final subject list:', list);
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

    // Formater les dates pour l'API (YYYY-MM-DD)
    const startDate = this.weekStart.toISOString().split('T')[0];
    const endDate   = weekEnd.toISOString().split('T')[0];

    // Charger sessions (filtrées par semaine), objectifs ET tous les subjects en parallèle
    forkJoin({
      sessions: this.svc.getByDateRange(startDate, endDate).pipe(catchError(() => of([]))),
      objectives: this.objectiveSvc.getAll().pipe(catchError(() => of([]))),
      allSubjects: this.subjectSvc.getAll().pipe(catchError(() => of([])))
    }).subscribe({
      next: (res) => {
        const all = res.sessions || [];
        const objectives = res.objectives || [];
        const allSubjects = res.allSubjects || [];
        console.log(`Planning load - Week ${startDate} to ${endDate}: Sessions: ${all.length}, Objectives: ${objectives.length}, All Subjects: ${allSubjects.length}`);
        console.log('📥 Raw sessions from API:', all);
        // ✅ Plus besoin de filtrer, le backend retourne déjà les sessions de la semaine
        this.blocks  = buildSessionBlocks(all, this.days);
        console.log('📊 After buildSessionBlocks:', this.blocks.length, 'blocks created');
        this.buildSubjectList(all, objectives, allSubjects);
        console.log('Subject list:', this.subjectList);
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        // Gestion des différentes erreurs HTTP
        if (err.status === 401 || err.status === 403) {
          this.error = '⚠️ Votre session a expiré. Veuillez vous reconnecter.';
          // Optionnel: redirection vers login
        } else if (err.status === 0) {
          this.error = '❌ Erreur réseau: Vérifiez votre connexion Internet.';
        } else if (err.status >= 500) {
          this.error = '❌ Erreur serveur (5xx). Veuillez réessayer dans quelques instants.';
        } else {
          this.error = err.message ?? 'Erreur de chargement des données';
        }
        this.loading = false;
        this.cdr.markForCheck();
      }
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

    // Formater les dates pour l'API (YYYY-MM-DD)
    const weekEnd = new Date(this.days[6].date);
    weekEnd.setHours(23, 59, 59, 999);
    const startDate = this.weekStart.toISOString().split('T')[0];
    const endDate   = weekEnd.toISOString().split('T')[0];

    // Générer sessions, puis charger objectifs ET subjects en parallèle
    forkJoin({
      sessions: this.svc.generate().pipe(catchError(() => of([]))),
      objectives: this.objectiveSvc.getAll().pipe(catchError(() => of([]))),
      allSubjects: this.subjectSvc.getAll().pipe(catchError(() => of([])))
    }).subscribe({
      next: (res) => {
        const sessions = res.sessions || [];
        const objectives = res.objectives || [];
        const allSubjects = res.allSubjects || [];
        this.days       = getWeekDays(this.weekStart);
        this.blocks     = buildSessionBlocks(sessions, this.days);
        this.buildSubjectList(sessions, objectives, allSubjects);
        this.generating = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        // Gestion des différentes erreurs HTTP
        if (err.status === 401 || err.status === 403) {
          this.error = '⚠️ Votre session a expiré. Veuillez vous reconnecter.';
        } else if (err.status === 429) {
          this.error = '⏱️ Trop de requêtes. Veuillez attendre quelques secondes avant de réessayer.';
        } else if (err.status === 402) {
          this.error = '💳 Quota dépassé. Veuillez mettre à jour votre plan.';
        } else if (err.status === 0) {
          this.error = '❌ Erreur réseau: Vérifiez votre connexion Internet.';
        } else if (err.status >= 500) {
          this.error = '❌ Erreur serveur (5xx). Veuillez réessayer dans quelques instants.';
        } else {
          this.error = err.message ?? 'Échec de la génération des sessions';
        }
        this.generating = false;
        this.cdr.markForCheck();
      }
    });
  }

  complete(b: SessionBlock, event: Event): void {
    event.stopPropagation();
    
    // Vérifier que la session existe
    if (!b.session.id) {
      this.error = 'Session introuvable';
      this.cdr.markForCheck();
      return;
    }
    
    // Vérifier le token avant d'envoyer la requête
    const token = localStorage.getItem('token');
    if (!token) {
      this.error = '🔐 Vous n\'êtes pas authentifié. Veuillez vous reconnecter.';
      this.cdr.markForCheck();
      return;
    }
    if (!token.includes('.')) {
      this.error = '🔐 Token invalide. Veuillez vous reconnecter.';
      this.cdr.markForCheck();
      return;
    }
    
    // Vérifier que la session est déjà complétée ou annulée
    if (b.status === 'DONE' || b.status === 'CANCELLED') {
      this.error = 'Cette session a déjà été traitée';
      this.cdr.markForCheck();
      return;
    }
    
    // Vérifier que la session est EN COURS (protection contre race condition)
    if (b.status !== 'ONGOING') {
      this.error = '⚠️ Vous pouvez compléter uniquement les sessions en cours. Cette session n\'a pas encore commencé.';
      this.cdr.markForCheck();
      return;
    }

    this.svc.complete(b.session.id).subscribe({
      next: updated => {
        b.status = updated.status as SessionStatus;
        this.error = null; // Effacer les erreurs précédentes
        this.cdr.markForCheck();
      },
      error: err => {
        console.error('❌ Complete error:', err);
        console.error('   Status:', err.status);
        console.error('   Error object:', err.error);
        console.error('   StatusText:', err.statusText);
        
        // Gestion des différentes erreurs HTTP
        if (err.status === 401) {
          this.error = '🔐 Authentification échouée (401). Votre token a expiré. Veuillez vous reconnecter.';
        } else if (err.status === 403) {
          this.error = '🔐 Accès refusé (403). Vous n\'avez pas les permissions pour compléter cette session.';
        } else if (err.status === 400) {
          // Extraire le message d'erreur du serveur s'il existe
          const serverMsg = err.error?.message || err.statusText || 'Requête invalide';
          this.error = `⚠️ Erreur (400): ${serverMsg}`;
        } else if (err.status === 404) {
          this.error = 'Session introuvable sur le serveur';
        } else if (err.status === 409) {
          // Conflit: le statut a changé depuis le dernier chargement (race condition)
          this.error = '⚠️ Le statut de la session a changé. Veuillez rafraîchir et réessayer.';
          // Recharger la session depuis le serveur
          this.load();
        } else if (err.status === 0) {
          this.error = '❌ Erreur réseau: Vérifiez votre connexion Internet.';
        } else if (err.status >= 500) {
          this.error = '❌ Erreur serveur (5xx). Veuillez réessayer dans quelques instants.';
        } else {
          this.error = `❌ Erreur (${err.status}): ${err.error?.message || err.statusText || 'Impossible de compléter la session'}`;
        }
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

    // Valider que startTime < endTime
    const startParts = this.availForm.startTime.split(':').map(Number);
    const endParts   = this.availForm.endTime.split(':').map(Number);
    const startMins  = startParts[0] * 60 + (startParts[1] || 0);
    const endMins    = endParts[0] * 60 + (endParts[1] || 0);

    if (startMins >= endMins) {
      this.availError = '⚠️ L\'heure de fin doit être après l\'heure de début.';
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
        // Gestion des différentes erreurs HTTP
        if (err.status === 401 || err.status === 403) {
          this.availError = '⚠️ Votre session a expiré. Veuillez vous reconnecter.';
        } else if (err.status === 400) {
          this.availError = '❌ Format d\'horaire invalide. Utilisez le format HH:MM.';
        } else if (err.status === 409) {
          this.availError = '⚠️ Cette disponibilité chevauche une session existante.';
        } else if (err.status === 0) {
          this.availError = '❌ Erreur réseau: Vérifiez votre connexion Internet.';
        } else if (err.status >= 500) {
          this.availError = '❌ Erreur serveur (5xx). Veuillez réessayer dans quelques instants.';
        } else {
          this.availError = err.message ?? "Erreur lors de l'ajout de la disponibilité";
        }
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
    // Valider que priority est dans l'intervalle [1, 5]
    if (!this.objectiveForm.priority || this.objectiveForm.priority < 1 || this.objectiveForm.priority > 5) {
      this.objectiveError = 'La priorité doit être entre 1 et 5.';
      return;
    }

    this.objectiveLoading = true;
    this.objectiveError   = null;

    this.objectiveSvc.create(this.objectiveForm).subscribe({
      next: () => {
        this.objectiveLoading = false;
        this.closeObjectiveModal();
        this.load(); // Recharger pour mettre à jour la liste des objectifs
        this.cdr.markForCheck();
      },
      error: err => {
        // Gestion des différentes erreurs HTTP
        if (err.status === 401 || err.status === 403) {
          this.objectiveError = '⚠️ Votre session a expiré. Veuillez vous reconnecter.';
        } else if (err.status === 400) {
          this.objectiveError = '❌ Les données de l\'objectif sont invalides.';
        } else if (err.status === 409) {
          this.objectiveError = '⚠️ Vous avez déjà un objectif pour cette matière cette semaine.';
        } else if (err.status === 0) {
          this.objectiveError = '❌ Erreur réseau: Vérifiez votre connexion Internet.';
        } else if (err.status >= 500) {
          this.objectiveError = '❌ Erreur serveur (5xx). Veuillez réessayer dans quelques instants.';
        } else {
          this.objectiveError = err.message ?? "Erreur lors de la création de l'objectif";
        }
        this.objectiveLoading = false;
        this.cdr.markForCheck();
      },
    });
    console.log('📤 Payload envoyé:', JSON.stringify(this.objectiveForm));
  }
}