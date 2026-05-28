import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BottomBarComponent } from './bottom-bar.component';

describe('BottomBarComponent', () => {
  let component: BottomBarComponent;
  let fixture: ComponentFixture<BottomBarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BottomBarComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(BottomBarComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
