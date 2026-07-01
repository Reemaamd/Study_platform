import { Component, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { forkJoin, of, from } from 'rxjs';
import { concatMap, catchError } from 'rxjs/operators';
import { OnboardingService } from '../../services/onboarding.service';
import { SubjectService } from '../../services/subject.service';
import { ObjectiveService } from '../../services/objective.service';

interface ObjectiveDraft {
  id: number;          // identifiant local temporaire (avant envoi backend)
  subjectIndex: number;
  title: string;
  weeklyGoal: number;  // défini à l'étape 3 (0 par défaut)
}

@Component({
  selector: 'app-onboarding',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './onboarding.component.html',
  styleUrls: ['./onboarding.component.css']
})
export class OnboardingComponent {

  @ViewChild('subjectInput') subjectInput!: ElementRef;
  @ViewChild('objectiveInput') objectiveInput!: ElementRef;

  step = 1;
  totalSteps = 6;
  loading = false;
  errorMessage = '';

  // ── STEP 1 — Subjects ──────────────────────────────
  subjects: string[] = [];
  newSubject = '';
  addingSubject = false;
  chipColors = ['#C5E8C8', '#F5D9A0', '#B8D4D8', '#F5C8C8', '#D8C8F5', '#C8E8F5'];

  // ── STEP 2 & 3 & 4 — Objectives (classement global) ─
  objectives: ObjectiveDraft[] = [];
  priorityOrder: number[] = [];
  private nextObjectiveId = 1;

  addingObjectiveFor: number | null = null;
  newObjectiveTitle = '';

  // ── STEP 5 — Availability grid ─────────────────────
  days = ['LUN', 'MAR', 'MER', 'JEU', 'VEN', 'SAM', 'DIM'];
  slots = ['MATIN', 'APRÈS-MIDI', 'SOIR'];
  selected: boolean[][] = Array.from({ length: 3 }, () => Array(7).fill(false));

  constructor(
    private router: Router,
    private onboardingService: OnboardingService,
    private subjectService: SubjectService,
    private objectiveService: ObjectiveService
  ) {}

  // ══════════════ STEP 1 — Matières ══════════════

  startAdding() {
    this.addingSubject = true;
    this.newSubject = '';
    setTimeout(() => this.subjectInput?.nativeElement?.focus(), 50);
  }

  confirmAdd() {
    const name = this.newSubject.trim();
    if (name) this.subjects.push(name);
    this.addingSubject = false;
    this.newSubject = '';
  }

  cancelAdd() {
    this.addingSubject = false;
    this.newSubject = '';
  }

  removeSubject(index: number) {
    const idsToRemove = this.objectives
      .filter(o => o.subjectIndex === index)
      .map(o => o.id);

    this.objectives = this.objectives.filter(o => o.subjectIndex !== index);
    this.priorityOrder = this.priorityOrder.filter(id => !idsToRemove.includes(id));

    this.objectives.forEach(o => {
      if (o.subjectIndex > index) o.subjectIndex--;
    });

    this.subjects.splice(index, 1);
  }

  // ══════════════ STEP 2 — Objectifs (titre seulement) ══

  objectivesFor(subjectIndex: number): ObjectiveDraft[] {
    return this.objectives.filter(o => o.subjectIndex === subjectIndex);
  }

  startAddingObjective(subjectIndex: number) {
    this.addingObjectiveFor = subjectIndex;
    this.newObjectiveTitle = '';
    setTimeout(() => this.objectiveInput?.nativeElement?.focus(), 50);
  }

  cancelAddObjective() {
    this.addingObjectiveFor = null;
  }

  confirmAddObjective(subjectIndex: number) {
    const title = this.newObjectiveTitle.trim();
    if (!title) return;

    const draft: ObjectiveDraft = {
      id: this.nextObjectiveId++,
      subjectIndex,
      title,
      weeklyGoal: 0 // sera défini à l'étape 3
    };

    this.objectives.push(draft);
    this.priorityOrder.push(draft.id);

    this.addingObjectiveFor = null;
  }

  removeObjective(id: number) {
    this.objectives = this.objectives.filter(o => o.id !== id);
    this.priorityOrder = this.priorityOrder.filter(oid => oid !== id);
  }

  get totalObjectivesCount(): number {
    return this.objectives.length;
  }

  // ══════════════ STEP 3 — Heures / semaine ══════════

  incrementGoal(id: number) {
    const o = this.objectives.find(o => o.id === id);
    if (o) o.weeklyGoal++;
  }

  decrementGoal(id: number) {
    const o = this.objectives.find(o => o.id === id);
    if (o && o.weeklyGoal > 0) o.weeklyGoal--;
  }

  hoursFor(subjectIndex: number): number {
    return this.objectivesFor(subjectIndex).reduce((acc, o) => acc + o.weeklyGoal, 0);
  }

  get totalHours(): number {
    return this.objectives.reduce((acc, o) => acc + (o.weeklyGoal || 0), 0);
  }

  // toutes les heures doivent être > 0 pour continuer
  get allGoalsSet(): boolean {
    return this.objectives.length > 0 && this.objectives.every(o => o.weeklyGoal > 0);
  }

  // ══════════════ STEP 4 — Priorité (classement global) ══

  // renvoie tous les objectifs triés par priorité actuelle
  get rankedObjectives(): ObjectiveDraft[] {
    return [...this.objectives].sort((a, b) => this.priorityOf(a.id) - this.priorityOf(b.id));
  }

  subjectNameOf(o: ObjectiveDraft): string {
    return this.subjects[o.subjectIndex] ?? '';
  }

// affichage utilisateur (1 = le plus important, inchangé)
priorityOf(id: number): number {
  return this.priorityOrder.indexOf(id) + 1;
}

// valeur RÉELLEMENT envoyée au backend (inversée : le + important reçoit le plus grand nombre)
backendPriorityOf(id: number): number {
  const total = this.priorityOrder.length;
  return total - this.priorityOrder.indexOf(id); // #1 affiché → N envoyé, #N affiché → 1 envoyé
}

  canMoveUp(id: number): boolean {
    return this.priorityOrder.indexOf(id) > 0;
  }

  canMoveDown(id: number): boolean {
    const idx = this.priorityOrder.indexOf(id);
    return idx >= 0 && idx < this.priorityOrder.length - 1;
  }

  moveUp(id: number) {
    const idx = this.priorityOrder.indexOf(id);
    if (idx > 0) {
      [this.priorityOrder[idx - 1], this.priorityOrder[idx]] =
        [this.priorityOrder[idx], this.priorityOrder[idx - 1]];
    }
  }

  moveDown(id: number) {
    const idx = this.priorityOrder.indexOf(id);
    if (idx >= 0 && idx < this.priorityOrder.length - 1) {
      [this.priorityOrder[idx + 1], this.priorityOrder[idx]] =
        [this.priorityOrder[idx], this.priorityOrder[idx + 1]];
    }
  }

  // ══════════════ STEP 5 — Disponibilités ══════════════

  isSelected(slotIndex: number, dayIndex: number): boolean {
    return this.selected[slotIndex]?.[dayIndex] ?? false;
  }

  toggleSlot(slotIndex: number, dayIndex: number) {
    this.selected[slotIndex][dayIndex] = !this.selected[slotIndex][dayIndex];
  }

  get totalSlots(): number {
    return this.selected.flat().filter(Boolean).length;
  }

  // ══════════════ Navigation ══════════════

  next() {
    if (this.step === 1 && this.subjects.length === 0) return;
    if (this.step === 2 && this.totalObjectivesCount === 0) return;
    if (this.step === 3 && !this.allGoalsSet) return;
    if (this.step === 5 && this.totalSlots === 0) return;
    if (this.step < this.totalSteps) this.step++;
  }

  prev() {
    if (this.step > 1) this.step--;
  }

  // ══════════════ STEP 6 — Récap + envoi final ══════════════

  finish() {
    this.loading = true;
    this.errorMessage = '';

    const dayMap: { [key: string]: string } = {
      'LUN': 'MONDAY', 'MAR': 'TUESDAY', 'MER': 'WEDNESDAY',
      'JEU': 'THURSDAY', 'VEN': 'FRIDAY', 'SAM': 'SATURDAY', 'DIM': 'SUNDAY'
    };
    const slotTimeMap: { [key: string]: { start: string; end: string } } = {
      'MATIN': { start: '08:00', end: '12:00' },
      'APRÈS-MIDI': { start: '14:00', end: '18:00' },
      'SOIR': { start: '19:00', end: '22:00' }
    };

    const availability: { day: string; startTime: string; endTime: string }[] = [];
    this.slots.forEach((slot, si) => {
      this.days.forEach((day, di) => {
        if (this.selected[si][di]) {
          const times = slotTimeMap[slot];
          availability.push({ day: dayMap[day], startTime: times.start, endTime: times.end });
        }
      });
    });

    from(this.subjects).pipe(
      concatMap((name, subjectIndex) =>
        this.subjectService.createSubject({ name }).pipe(
          concatMap(subject => {
            const drafts = this.objectivesFor(subjectIndex);
            if (!drafts.length) return of(subject);

            return forkJoin(
              drafts.map(o =>
                this.objectiveService.create({
  subjectId: subject.id,
  title: o.title,
  weeklyGoal: o.weeklyGoal,
  priority: this.backendPriorityOf(o.id) // ← inversé, pas priorityOf()
})
              )
            ).pipe(concatMap(() => of(subject)));
          })
        )
      ),
      catchError(err => {
        console.error('❌ Erreur création matières/objectifs:', err);
        this.errorMessage = "Une erreur est survenue lors de l'enregistrement.";
        this.loading = false;
        throw err;
      })
    ).subscribe({
      complete: () => {
        this.onboardingService.saveOnboarding({ availability }).subscribe({
          next: () => {
            this.loading = false;
            this.router.navigate(['/dashboard']); // ← le backend génère le planning ici
          },
          error: (err: any) => {
            console.error('❌ Erreur disponibilités:', err);
            this.errorMessage = "Une erreur est survenue lors de l'enregistrement des disponibilités.";
            this.loading = false;
          }
        });
      }
    });
  }
}