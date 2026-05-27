import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class WeekGuardService {

  private isBrowser: boolean;

  constructor(
    private router: Router,
    @Inject(PLATFORM_ID) platformId: object,
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  getCurrentWeekKey(): string {
    const now    = new Date();
    const monday = new Date(now);
    monday.setDate(now.getDate() - ((now.getDay() + 6) % 7));
    return monday.toISOString().slice(0, 10);
  }

  private storageKey(): string {
    if (!this.isBrowser) return '';
    const username = localStorage.getItem('username') || 'guest';
    return `onboarding_week_${username}`;
  }

  isNewWeek(): boolean {
    if (!this.isBrowser) return false;
    return localStorage.getItem(this.storageKey()) !== this.getCurrentWeekKey();
  }

  markWeekDone(): void {
    if (!this.isBrowser) return;
    localStorage.setItem(this.storageKey(), this.getCurrentWeekKey());
  }

  /*/ ✅ NE PAS appeler depuis AppComponent — seulement depuis login.component.ts
  checkAndRedirect(): void {
    if (!this.isBrowser) return;
    const isLoggedIn = !!localStorage.getItem('token');
    if (!isLoggedIn) return;
    if (this.isNewWeek()) {
      this.router.navigate(['/weekly-onboarding']);
    }
  }*/
}