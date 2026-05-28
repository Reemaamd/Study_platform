import { Routes } from '@angular/router';

export const routes: Routes = [

  {
    path: '',
    loadComponent: () =>
      import('./landing/landing.component')
        .then(m => m.LandingComponent),
  },

  {
    path: 'login',
    loadComponent: () =>
      import('./pages/login/login.component')
        .then(m => m.LoginComponent),
  },

  {
    path: 'register',
    loadComponent: () =>
      import('./pages/register/register.component')
        .then(m => m.RegisterComponent),
  },

  {
    path: 'dashboard',
    loadComponent: () =>
      import('./pages/dashboard/dashboard.component')
        .then(m => m.DashboardComponent),
  },

  {
    path: 'analytics',
    loadComponent: () =>
      import('./pages/analytics/analytics.component')
        .then(m => m.AnalyticsComponent),
  },

  {
    path: 'groups',
    loadComponent: () =>
      import('./pages/groups/groups.component')
        .then(m => m.GroupsComponent),
  },

  {
    path: 'planning',
    loadComponent: () =>
      import('./pages/planning/planning.component')
        .then(m => m.PlanningComponent),
  },

  {
    path: 'notification',
    loadComponent: () =>
      import('./pages/notification/notification.component')
        .then(m => m.NotificationComponent),
  },

  {
    path: 'settings',
    loadComponent: () =>
      import('./pages/settings/settings.component')
        .then(m => m.SettingsComponent),
  },

  {
    path: '**',
    redirectTo: '',
  },

];