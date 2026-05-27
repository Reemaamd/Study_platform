import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { NotificationService, AppNotification } from '../../services/notification.service';
import { ChangeDetectorRef } from '@angular/core';
import { BottomNavComponent } from '../../components/bottom-bar/bottom-bar.component';

interface NotificationGroup {
  label: string;
  items: AppNotification[];
}

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule, BottomNavComponent],
  templateUrl: './notifications.component.html',
  styleUrls: ['./notifications.component.css'],
})
export class NotificationsComponent implements OnInit {
  groups: NotificationGroup[] = [];
  loading    = true;
  markingAll = false;

  constructor(private notifService: NotificationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.load();
  }

  // ─── Chargement ────────────────────────────────────────────
  load(): void {
  this.loading = true;

  this.notifService.getMyNotifications().subscribe({
    next: (res: any) => {

      console.log("RAW =", res);

      const notifs = Array.isArray(res)
  ? res
  : (res?.data || res?.content || []);
  
      // 🔥 ICI TU AJOUTES TES LOGS
      console.log("FIRST NOTIF =", notifs[0]);
      console.log("DATE =", new Date(notifs[0]?.createdAt));
      console.log("NOTIFS =", notifs);

      this.groups = this.groupByDate([...notifs]); // important spread

      console.log("GROUPS =", this.groups);

      this.loading = false;
      this.cdr.detectChanges(); // 🔥 IMPORTANT
    },
    error: (err) => {
      console.error(err);
      this.loading = false;
    }
  });
}

  // ─── Marquer UNE notification comme lue ────────────────────
  // Appelé au clic sur une card
  markRead(notif: AppNotification): void {
    if (notif.isRead) return; // déjà lue → rien à faire

    // Appel PUT /notifications/{id}/read
    this.notifService.markAsRead(notif.id).subscribe({
      next: () => {
        // Mise à jour locale immédiate — pas besoin de recharger toute la liste
        notif.isRead = true;
      },
      error: (err) => {
        console.error('Erreur markAsRead pour', notif.id, err);
      },
    });
  }

  // ─── Marquer TOUTES les notifications comme lues ───────────
  //
  // Le backend n'a PAS d'endpoint "mark all read".
  // On simule ce comportement côté frontend en envoyant
  // N requêtes PUT /{id}/read en parallèle (une par notification non lue).
  //
  // Avantages :
  //   • Fonctionne sans modifier le backend
  //   • Les requêtes partent en parallèle (pas en série) → rapide
  //
  // Inconvénient :
  //   • Si tu as 100 notifs non lues → 100 requêtes HTTP
  //   • Solution long terme : ajouter PUT /notifications/read-all au backend
  //
  markAllRead(): void {
  const username = localStorage.getItem('username'); // ou depuis JWT

  if (!username) return;

  this.markingAll = true;

  this.notifService.markAllAsRead(username).subscribe({
    next: () => {
      this.groups.forEach(g =>
        g.items.forEach(n => n.isRead = true)
      );

      this.markingAll = false;
    },
    error: (err) => {
      console.error('markAllRead error', err);
      this.markingAll = false;
    }
  });
}

  // ─── Helpers ───────────────────────────────────────────────
  allNotifications(): AppNotification[] {
    return this.groups.flatMap((g) => g.items);
  }

  unreadCount(): number {
    return this.allNotifications().filter((n) => !n.isRead).length;
  }

  typeLabel(type: string): string {
    const map: Record<string, string> = {
      SESSION_REMINDER_15: 'Rappel 15 min',
      SESSION_REMINDER_5:  'Rappel 5 min',
      PLANNING_GENERATED:  'Planning',
      OBJECTIVE_COMPLETED: 'Objectif atteint',
    };
    return map[type] ?? type;
  }

  cardClass(type: string): string {
    const map: Record<string, string> = {
      SESSION_REMINDER_15: 'notif-rappel',
      SESSION_REMINDER_5:  'notif-warning',
      PLANNING_GENERATED:  'notif-planning',
      OBJECTIVE_COMPLETED: 'notif-objectif',
    };
    return map[type] ?? 'notif-default';
  }

  typeIcon(type: string): string {
    const map: Record<string, string> = {
      SESSION_REMINDER_15: '⏰',
      SESSION_REMINDER_5:  '⚠️',
      PLANNING_GENERATED:  '📅',
      OBJECTIVE_COMPLETED: '🎯',
    };
    return map[type] ?? '🔔';
  }

  formatDate(dateStr: string): string {
    const d = new Date(dateStr);
    return d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  }

  // ─── Groupement par date ───────────────────────────────────
  private groupByDate(notifs: AppNotification[]): NotificationGroup[] {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const yesterday = new Date(today);
  yesterday.setDate(today.getDate() - 1);

  const groups: Record<string, AppNotification[]> = {};

  notifs.forEach((n) => {
    // Fonctionne avec string ISO ET timestamp numérique
    const d = new Date(n.createdAt ?? Date.now());
    
    // Sécurité : si la date est invalide, on met "Autres"
    if (isNaN(d.getTime())) {
      if (!groups['Autres']) groups['Autres'] = [];
      groups['Autres'].push(n);
      return;
    }

    d.setHours(0, 0, 0, 0);
      let label: string;
      if      (d.getTime() === today.getTime())     label = "Aujourd'hui";
      else if (d.getTime() === yesterday.getTime()) label = 'Hier';
      else    label = d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'long' });

      if (!groups[label]) groups[label] = [];
      groups[label].push(n);
    });

    const order  = ["Aujourd'hui", 'Hier'];
    const sorted = Object.keys(groups).sort((a, b) => {
      const ia = order.indexOf(a);
      const ib = order.indexOf(b);
      if (ia !== -1 && ib !== -1) return ia - ib;
      if (ia !== -1) return -1;
      if (ib !== -1) return  1;
      return 0;
    });

    return sorted.map((label) => ({ label, items: groups[label] }));
  }
}
