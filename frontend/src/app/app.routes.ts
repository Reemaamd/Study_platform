import { Routes } from '@angular/router';
// Use lazy-loaded standalone components to avoid direct imports that may fail
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { SettingsComponent } from './pages/user-settings/settings.component';
import { AnalyticsComponent } from './pages/analytics/analytics.component';
import { PlanningComponent } from './pages/planning/planning.component';
import { authGuard } from './guards/auth.guard';
import { ForgotPasswordComponent } from './pages/forgot-password/forgot-password.component';
import { VerifyCodeComponent } from './pages/verify-code/verify-code.component';
import { ResetPasswordComponent } from './pages/reset-password/reset-password.component';

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
    path: 'forgot-password',
    component: ForgotPasswordComponent
},
{
    path: 'verify-code',
    component: VerifyCodeComponent
},
{
    path: 'reset-password',
    component: ResetPasswordComponent
},

  {
    path: 'register',
    loadComponent: () =>
      import('./pages/register/register.component')
        .then(m => m.RegisterComponent),
  },

     {
    path: 'analytics',
    component: AnalyticsComponent
  },

  {
    path: 'groups',
    loadComponent: () =>
      import('./pages/groups/groups.component')
        .then(m => m.GroupsComponent),
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
    component: PlanningComponent
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
 { path: 'settings', component: SettingsComponent, canActivate: [authGuard] },
  {
    path: '**',
    redirectTo: '',
  },
  

];