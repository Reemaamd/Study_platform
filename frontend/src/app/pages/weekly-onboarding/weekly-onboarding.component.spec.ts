import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WeeklyOnboardingComponent } from './weekly-onboarding.component';

describe('WeeklyOnboardingComponent', () => {
  let component: WeeklyOnboardingComponent;
  let fixture: ComponentFixture<WeeklyOnboardingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WeeklyOnboardingComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(WeeklyOnboardingComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
