import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { ChangeDetectorRef } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { Router } from '@angular/router';
import { catchError, finalize } from 'rxjs/operators';

type AdminTab = 'overview' | 'users' | 'groups' | 'account';

interface AdminDashboard {
  totalUsers: number;
  activeUsers: number;
  totalSessions: number;
  completedSessions: number;
  totalStudyHours: number;
  avgStudyPerUser: number;
}
interface FullUser {
  id: string; name: string; username: string;
  email: string; role: string;
  sessions: number; completedSessions: number;
  studyHours: number; active: boolean;
}
interface UserStat {
  userId: string; username: string;
  sessions: number; completedSessions: number; studyHours: number;
}
interface SubjectStat {
  subject: string; totalSessions: number; totalHours: number;
}
interface AdminGroup {
  id: string; name: string;
  ownerUsername: string; ownerEmail: string;
  memberCount: number; createdAt?: string;
  status: 'ACTIVE' | 'INACTIVE';  // depuis AdminGroupService : ACTIVE si msg < 7j
}
interface MyProfile {
  name: string; username: string;
  email: string; role: string | { name: string };
}
interface DisplayUser extends FullUser {
  status: 'active' | 'pause' | 'inactif';
}
interface Notif { type: 'success' | 'error'; message: string; }

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.css']
})
export class AdminComponent implements OnInit {

  activeTab = signal<AdminTab>('overview');
  private readonly baseUrl = 'http://localhost:8080';

  // ── OVERVIEW
  overviewStats: AdminDashboard = {
    totalUsers: 0, activeUsers: 0,
    totalSessions: 0, completedSessions: 0,
    totalStudyHours: 0, avgStudyPerUser: 0,
  };
  subjectStats: SubjectStat[]  = [];
  activityData: number[]       = Array(12).fill(0);
  activityLabels: string[]     = Array(12).fill('');
  roleDistribution             = { users: 0, admins: 0 };
  roleUserPct  = 0;
  roleAdminPct = 0;
  overviewLoading = false;

  // ── USERS
  users: DisplayUser[]         = [];
  filteredUsers: DisplayUser[] = [];
  userSearchQuery = '';
  roleFilter      = 'all';
  showInviteModal = false;
  inviteForm      = { name: '', username: '', email: '', password: '', role: 'USER' };
  editingUser: DisplayUser | null = null;
  editForm        = { name: '', email: '' };
  usersLoading    = false;

  // ── GROUPS
  groups: AdminGroup[]         = [];
  filteredGroups: AdminGroup[] = [];
  groupSearchQuery = '';
  groupsLoading    = false;

  // ── ACCOUNT
  account       = { name: '', username: '', email: '', role: '' };
  passwordForm  = { current: '', newPwd: '', confirm: '' };
  accountEditMode = false;
  pwdError        = '';
  pwdSuccess      = '';
  accountSuccess  = '';

  // ── TOAST
  notif: Notif | null = null;
  private notifTimer: any;

  constructor(private http: HttpClient, private cd: ChangeDetectorRef, private router: Router) {}

  ngOnInit(): void {
    this.loadOverview();
    this.loadUsers();
    this.loadAccount();
  }

  private h(): HttpHeaders {
    return new HttpHeaders({ Authorization: `Bearer ${localStorage.getItem('token') || ''}` });
  }

  setTab(tab: AdminTab): void {
    this.activeTab.set(tab);
    if (tab === 'overview') this.loadOverview();
    if (tab === 'users')    this.loadUsers();
    if (tab === 'groups')   this.loadGroups();
    if (tab === 'account')  this.loadAccount();
  }

  private showNotif(type: 'success' | 'error', message: string): void {
    if (this.notifTimer) clearTimeout(this.notifTimer);
    this.notif = { type, message };
    this.cd.detectChanges();
    this.notifTimer = setTimeout(() => { this.notif = null; this.cd.detectChanges(); }, 3500);
  }

  // ══════════════════════════════════════════════════════════════
  // OVERVIEW — forkJoin avec catchError sur chaque call
  // → les deux requêtes partent en parallèle
  // → overviewLoading passe à false quand les DEUX finissent
  //   même si l'une échoue
  // ══════════════════════════════════════════════════════════════
  loadOverview(): void {
    this.overviewLoading = true;

    const dashboard$ = this.http
      .get<AdminDashboard>(`${this.baseUrl}/statistics/admin/dashboard`, { headers: this.h() })
      .pipe(catchError(() => of(null)));

    const subjects$ = this.http
      .get<SubjectStat[]>(`${this.baseUrl}/statistics/admin/subjects-stats`, { headers: this.h() })
      .pipe(catchError(() => of([])));

    forkJoin({ dashboard: dashboard$, subjects: subjects$ })
      .pipe(finalize(() => { this.overviewLoading = false; this.cd.detectChanges(); }))
      .subscribe(({ dashboard, subjects }) => {

        if (dashboard) this.overviewStats = { ...dashboard };

        const subjectList = (subjects as SubjectStat[])
          .sort((a, b) => b.totalHours - a.totalHours)
          .slice(0, 12);
        this.subjectStats   = subjectList;
        this.activityData   = subjectList.map(s => s.totalHours);
        this.activityLabels = subjectList.map(s =>
          s.subject.length > 6 ? s.subject.slice(0, 6) : s.subject
        );
        while (this.activityData.length   < 12) this.activityData.unshift(0);
        while (this.activityLabels.length < 12) this.activityLabels.unshift('');
      });
  }

  get maxActivity(): number { return Math.max(...this.activityData, 1); }
  barHeight(v: number): number { return Math.round((v / this.maxActivity) * 140); }
  get topSubjects(): SubjectStat[] { return this.subjectStats.slice(0, 5); }

  // ══════════════════════════════════════════════════════════════
  // USERS — forkJoin /admin + /users-stats avec finalize garanti
  // ══════════════════════════════════════════════════════════════
  loadUsers(): void {
    this.usersLoading = true;

    // Essai endpoint enrichi
    const allUsers$ = this.http
      .get<FullUser[]>(`${this.baseUrl}/statistics/admin/all-users`, { headers: this.h() })
      .pipe(catchError(() => of(null)));

    // Fallback parts
    const baseUsers$ = this.http
      .get<any[]>(`${this.baseUrl}/admin`, { headers: this.h() })
      .pipe(catchError(() => of([])));

    const userStats$ = this.http
      .get<UserStat[]>(`${this.baseUrl}/statistics/admin/users-stats`, { headers: this.h() })
      .pipe(catchError(() => of([])));

    forkJoin({ allUsers: allUsers$, baseUsers: baseUsers$, stats: userStats$ })
      .pipe(finalize(() => { this.usersLoading = false; this.cd.detectChanges(); }))
      .subscribe(({ allUsers, baseUsers, stats }) => {

        if (allUsers && allUsers.length > 0) {
          // Endpoint enrichi disponible → utilisation directe
          this.users = (allUsers as FullUser[]).map(u => ({ ...u, status: this.deriveStatus(u) }));
        } else {
          // Fallback : fusionner /admin + /users-stats
          const mapped: DisplayUser[] = (baseUsers as any[]).map((u: any) => ({
            id: u.id || '', name: u.name || '',
            username: u.username || '', email: u.email || '',
            role: u.role || 'USER',
            sessions: 0, completedSessions: 0,
            studyHours: 0, active: false, status: 'inactif' as const,
          }));
          (stats as UserStat[]).forEach(s => {
            const u = mapped.find(x => x.id === s.userId || x.username === s.username);
            if (!u) return;
            u.sessions          = s.sessions          ?? 0;
            u.completedSessions = s.completedSessions ?? 0;
            u.studyHours        = s.studyHours        ?? 0;
            u.active            = u.studyHours > 0;
            u.status            = this.deriveStatus(u);
          });
          this.users = mapped;
        }

        this.buildRoleDistribution();
        this.applyUserFilter();
        // Charger groupes après users pour que le fallback puisse résoudre ownerId
        //this.loadGroups();
      });
  }

  private deriveStatus(u: FullUser): 'active' | 'pause' | 'inactif' {
    if (u.active || u.studyHours > 20)        return 'active';
    if (u.studyHours > 0 || u.sessions > 0)   return 'pause';
    return 'inactif';
  }

  private buildRoleDistribution(): void {
    this.roleDistribution.admins = this.users.filter(u =>
      u.role === 'ADMIN' || u.role === 'ROLE_ADMIN').length;
    this.roleDistribution.users  = this.users.filter(u =>
      u.role === 'USER'  || u.role === 'ROLE_USER').length;
    const total = this.users.length || 1;
    this.roleUserPct  = Math.round((this.roleDistribution.users  / total) * 100);
    this.roleAdminPct = Math.round((this.roleDistribution.admins / total) * 100);
  }

  applyUserFilter(): void {
    const q = this.userSearchQuery.trim().toLowerCase();
    this.filteredUsers = this.users.filter(u => {
      const matchRole = this.roleFilter === 'all'
        || u.role === this.roleFilter
        || u.role === 'ROLE_' + this.roleFilter;
      const matchQ = !q
        || u.name.toLowerCase().includes(q)
        || u.email.toLowerCase().includes(q)
        || u.username.toLowerCase().includes(q);
      return matchRole && matchQ;
    });
  }

  onUserSearchInput(e: Event): void {
    this.userSearchQuery = (e.target as HTMLInputElement).value;
    this.applyUserFilter();
  }
  onRoleFilterChange(e: Event): void {
    this.roleFilter = (e.target as HTMLSelectElement).value;
    this.applyUserFilter();
  }

  getInitials(name: string): string {
    if (!name) return '?';
    return name.split(' ').map(p => p[0]).join('').slice(0, 2).toUpperCase();
  }

  deleteUser(id: string): void {
    if (!confirm('Supprimer cet utilisateur ?')) return;
    this.http.delete(`${this.baseUrl}/admin/${id}`, {
      headers: this.h(), responseType: 'text' as 'json'
    }).subscribe({
      next: () => { this.showNotif('success', '✓ Utilisateur supprimé'); this.loadUsers(); },
      error: ()  => this.showNotif('error', '✗ Erreur suppression')
    });
  }

  openEdit(u: DisplayUser): void { this.editingUser = u; this.editForm = { name: u.name, email: u.email }; }
  closeEdit(): void { this.editingUser = null; }

  submitEdit(): void {
    if (!this.editingUser) return;
    this.http.put<any>(
      `${this.baseUrl}/admin/${this.editingUser.id}`, this.editForm, { headers: this.h() }
    ).subscribe({
      next: () => { this.closeEdit(); this.showNotif('success', '✓ Modifié'); this.loadUsers(); },
      error: ()  => this.showNotif('error', '✗ Erreur modification')
    });
  }

  openInvite():  void { this.showInviteModal = true; }
  closeInvite(): void {
    this.showInviteModal = false;
    this.inviteForm = { name: '', username: '', email: '', password: '', role: 'USER' };
  }
  submitInvite(): void {
    this.http.post<FullUser>(`${this.baseUrl}/admin`, this.inviteForm, { headers: this.h() }).subscribe({
      next: () => { this.closeInvite(); this.showNotif('success', '✓ Compte créé'); this.loadUsers(); },
      error: ()  => this.showNotif('error', '✗ Erreur création')
    });
  }

  // ══════════════════════════════════════════════════════════════
  // GROUPS — finalize garanti, fallback enrichi depuis this.users
  // ══════════════════════════════════════════════════════════════
  loadGroups(): void {
  this.groupsLoading = true;

  this.http
    .get<AdminGroup[]>(`${this.baseUrl}/admin/groups`, { headers: this.h() })
    .pipe(
      catchError(() => of([] as AdminGroup[])),
      finalize(() => { this.groupsLoading = false; this.cd.detectChanges(); })
    )
    .subscribe(groups => {
      this.groups = groups;
      this.filteredGroups = [...this.groups];
    });
}

  applyGroupFilter(): void {
  const q = this.groupSearchQuery.trim().toLowerCase();

  this.filteredGroups = !q
    ? [...this.groups]
    : this.groups.filter(g =>
        (g.name ?? '').toLowerCase().includes(q) ||
        (g.ownerUsername ?? '').toLowerCase().includes(q) ||
        (g.ownerEmail ?? '').toLowerCase().includes(q)
      );
}

  onGroupSearchInput(e: Event): void {
    this.groupSearchQuery = (e.target as HTMLInputElement).value;
    this.applyGroupFilter();
  }

  formatDate(d?: string): string {
    if (!d) return '—';
    try { return new Date(d).toLocaleDateString('fr-FR', { day:'2-digit', month:'short', year:'numeric' }); }
    catch { return '—'; }
  }

  // ══════════════════════════════════════════════════════════════
  // ACCOUNT
  // ══════════════════════════════════════════════════════════════
  loadAccount(): void {
    this.http.get<MyProfile>(`${this.baseUrl}/users/me`, { headers: this.h() })
      .pipe(catchError(err => {
        if (err.status === 403) {
          this.account = {
            name: '⚠ Accès refusé', username: '',
            email: 'Corriger @PreAuthorize dans UserController', role: 'ADMIN'
          };
        }
        return of(null);
      }))
      .subscribe(u => {
        if (!u) return;
        this.account = {
          name:     u.name     ?? '',
          username: u.username ?? '',
          email:    u.email    ?? '',
          role:     this.roleStr(u.role),
        };
      });
  }

  private roleStr(r: any): string {
    if (!r) return '';
    if (typeof r === 'string') return r.replace('ROLE_', '');
    if (r?.name) return String(r.name).replace('ROLE_', '');
    return String(r);
  }

  saveAccount(): void {
    this.http.put<MyProfile>(
      `${this.baseUrl}/users/me`,
      { name: this.account.name, email: this.account.email },
      { headers: this.h() }
    ).subscribe({
      next: (u) => {
        if (u) { this.account.name = u.name ?? this.account.name; this.account.email = u.email ?? this.account.email; }
        this.accountSuccess  = 'Informations mises à jour.';
        this.accountEditMode = false;
        setTimeout(() => this.accountSuccess = '', 3000);
      },
      error: () => this.showNotif('error', '✗ Erreur mise à jour')
    });
  }

  changePassword(): void {
    this.pwdError = ''; this.pwdSuccess = '';
    if (this.passwordForm.newPwd !== this.passwordForm.confirm) { this.pwdError = 'Mots de passe différents.'; return; }
    if (this.passwordForm.newPwd.length < 8) { this.pwdError = 'Minimum 8 caractères.'; return; }

    const params = new HttpParams()
      .set('username',    this.account.username)
      .set('oldPassword', this.passwordForm.current)
      .set('newPassword', this.passwordForm.newPwd);

    this.http.put(`${this.baseUrl}/users/change-password`, {}, {
      headers: this.h(), params, responseType: 'text'
    }).subscribe({
      next: (res) => {
        this.pwdSuccess = typeof res === 'string' ? res : 'Mot de passe mis à jour.';
        this.passwordForm = { current: '', newPwd: '', confirm: '' };
        setTimeout(() => { this.pwdSuccess = ''; this.cd.detectChanges(); }, 4000);
      },
      error: (e) => { this.pwdError = e.error?.message || e.error || 'Mot de passe actuel incorrect.'; }
    });
  }

  // ══════════════════════════════════════════════════════════════
  // LOGOUT — vide le token et redirige vers la landing page
  // ══════════════════════════════════════════════════════════════
  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('username');
    this.router.navigate(['/']);
  }

}