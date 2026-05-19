import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LandingComponent } from './landing.component';

describe('LandingComponent', () => {
  let component: LandingComponent;
  let fixture: ComponentFixture<LandingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LandingComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(LandingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have 4 planning sessions', () => {
    expect(component.sessions.length).toBe(4);
  });

  it('should have 5 feature cards', () => {
    expect(component.features.length).toBe(5);
  });

  it('should render hero title', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.hero-title')?.textContent).toContain('intention');
  });

  it('should render the planning card sessions', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const sessions = compiled.querySelectorAll('.session');
    expect(sessions.length).toBe(4);
  });

  it('should render the CTA banner', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.cta-banner')).toBeTruthy();
  });
});