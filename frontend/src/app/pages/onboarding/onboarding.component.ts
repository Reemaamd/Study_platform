import { Component, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { OnboardingService } from '../../services/onboarding.service';

@Component({
  selector: 'app-onboarding',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './onboarding.component.html',
  styleUrls: ['./onboarding.component.css']
})
export class OnboardingComponent {

  @ViewChild('subjectInput') subjectInput!: ElementRef;

  step = 1;
  loading = false;

  // ── STEP 1 — Subjects ──────────────────────────────
  subjects: string[] = [];
  newSubject = '';
  addingSubject = false;

  chipColors = ['#C5E8C8', '#F5D9A0', '#B8D4D8', '#F5C8C8', '#D8C8F5', '#C8E8F5'];

  // ── STEP 2 — Objectives (hours per week per subject) ─
  objectives: number[] = [];

  // ── STEP 3 — Availability grid ─────────────────────
  days = ['LUN', 'MAR', 'MER', 'JEU', 'VEN', 'SAM', 'DIM'];
  slots = ['MATIN', 'APRÈS-MIDI', 'SOIR'];

  // selected[slotIndex][dayIndex] = true/false
  selected: boolean[][] = Array.from({ length: 3 }, () => Array(7).fill(false));

  constructor(
    private router: Router,
    private onboardingService: OnboardingService
  ) {}

  // ── STEP 1 methods ────────────────────────────────

  startAdding() {
    this.addingSubject = true;
    this.newSubject = '';
    setTimeout(() => this.subjectInput?.nativeElement?.focus(), 50);
  }

  confirmAdd() {
    const name = this.newSubject.trim();
    if (name) {
      this.subjects.push(name);
      this.objectives.push(4); // default 4h
    }
    this.addingSubject = false;
    this.newSubject = '';
  }

  cancelAdd() {
    this.addingSubject = false;
    this.newSubject = '';
  }

  removeSubject(index: number) {
    this.subjects.splice(index, 1);
    this.objectives.splice(index, 1);
  }

  // ── STEP 2 methods ────────────────────────────────

  setObjective(index: number, event: Event) {
    const val = parseInt((event.target as HTMLInputElement).value, 10);
    this.objectives[index] = val;
  }

  get totalHours(): number {
    return this.objectives.reduce((acc, h) => acc + (h || 0), 0);
  }

  // ── STEP 3 methods ────────────────────────────────

  isSelected(slotIndex: number, dayIndex: number): boolean {
    return this.selected[slotIndex]?.[dayIndex] ?? false;
  }

  toggleSlot(slotIndex: number, dayIndex: number) {
    this.selected[slotIndex][dayIndex] = !this.selected[slotIndex][dayIndex];
  }

  get totalSlots(): number {
    return this.selected.flat().filter(Boolean).length;
  }

  // ── Navigation ────────────────────────────────────

  next() {
    if (this.step === 1 && this.subjects.length === 0) return;
    this.step++;
  }

  prev() {
    if (this.step > 1) this.step--;
  }

  // ── Final submit ──────────────────────────────────

  finish() {
    this.loading = true;

    console.log('🔍 Token in localStorage:', localStorage.getItem('token') ? `${localStorage.getItem('token')!.substring(0, 20)}...` : 'NO TOKEN');

    // Map French day names to English enum values
    const dayMap: { [key: string]: string } = {
      'LUN': 'MONDAY',
      'MAR': 'TUESDAY',
      'MER': 'WEDNESDAY',
      'JEU': 'THURSDAY',
      'VEN': 'FRIDAY',
      'SAM': 'SATURDAY',
      'DIM': 'SUNDAY'
    };

    // Map slots to time ranges
    const slotTimeMap: { [key: string]: { start: string; end: string } } = {
      'MATIN': { start: '08:00', end: '12:00' },
      'APRÈS-MIDI': { start: '14:00', end: '18:00' },
      'SOIR': { start: '19:00', end: '22:00' }
    };

    // Build availability list from grid
    const availability: { day: string; startTime: string; endTime: string }[] = [];
    this.slots.forEach((slot, si) => {
      this.days.forEach((day, di) => {
        if (this.selected[si][di]) {
          const times = slotTimeMap[slot];
          availability.push({
            day: dayMap[day],  // Convert to English day name
            startTime: times.start,
            endTime: times.end
          });
        }
      });
    });

    // Build subjects + objectives payload
    const subjectsPayload = this.subjects.map((name, i) => ({
      name,
      title: name,
      weeklyGoal: this.objectives[i] || 1,
      priority: 2  // 1=LOW, 2=MEDIUM, 3=HIGH
    }));

    console.log('🎯 Sending subjects payload:', subjectsPayload);
    console.log('📅 Sending availability payload:', availability);

    this.onboardingService.saveOnboarding({
      subjects: subjectsPayload,
      availability
    }).subscribe({
      next: (response) => {
        console.log('✅ Onboarding success:', response);
        this.loading = false;
        this.router.navigate(['/dashboard']);
      },
      error: (err: any) => {
        console.error('❌ Onboarding error:', err);
        console.error('Status:', err.status);
        console.error('Message:', err.message);
        console.error('Error body:', err.error);
        this.loading = false;
      }
    });
  }
}