import { Routes } from '@angular/router';
// Use lazy-loaded standalone components to avoid direct imports that may fail
import { DashboardComponent } from './pages/dashboard/dashboard.component';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./landing/landing.component').then(m => m.LandingComponent),
  },
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () => import('./pages/register/register.component').then(m => m.RegisterComponent),
  },
    {
    path: 'dashboard',
    component: DashboardComponent
  },
  {
  path: 'onboarding',
  loadComponent: () =>
    import('./pages/onboarding/onboarding.component')
      .then(m => m.OnboardingComponent)
  },
  {
  path: 'planning',
  loadComponent: () =>
    import('./pages/planning/planning.component')
      .then(m => m.PlanningComponent)
  },
  {
  path: 'weekly-onboarding',
  loadComponent: () =>
    import('./pages/weekly-onboarding/weekly-onboarding.component')
      .then(m => m.WeeklyOnboardingComponent),
},
{
  path: 'notifications',
  loadComponent: () =>
    import('./pages/notifications/notifications.component')
      .then(m => m.NotificationsComponent),
},
  {
  path: 'admin',
  loadComponent: () => import('./pages/admin/admin.component').then(m => m.AdminComponent)
},
  {
    path: '**',
    redirectTo: '',
  }

];