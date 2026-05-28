import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { interval, Subscription } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { GroupsService } from '../../services/groups.service';

@Component({
  selector: 'app-groups',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './groups.component.html',
  styleUrls: ['./groups.component.css']
})
export class GroupsComponent implements OnInit, OnDestroy {

  // ───────────────── GROUPS ─────────────────

  groups: any[] = [];

  selectedGroup: any = null;

  loadingGroups = true;

  currentUsername =
    localStorage.getItem('username') || '';

  // ───────────────── CHAT ─────────────────

  messages: any[] = [];

  newMessage = '';

  sendingMsg = false;

  private pollSub?: Subscription;

  // ───────────────── NOTIFICATIONS ─────────────────

  notifications: any[] = [];

  // ───────────────── INVITATIONS ─────────────────

  invitations: any[] = [];

  // ───────────────── SESSIONS ─────────────────

  commonSlots: any[] = [];
  groupSessions: any[] = [];

loadingSessions = false;

  subjects: any[] = [];

  loadingSlots = false;

  showSessionModal = false;

  sessionSubjectId = '';

  sessionStartTime = '';

  sessionEndTime = '';

  sessionDay = '';

  creatingSession = false;

  sessionError = '';

  // ───────────────── MODALS ─────────────────

  showCreateModal = false;

  showInviteModal = false;

  showInvitListModal = false;

  newGroupName = '';

  inviteReceiverId = '';

  creatingGroup = false;

  sendingInvite = false;

  errorMsg = '';

  // ───────────────── TOAST ─────────────────

  toastMessage = '';

  toastType: 'success' | 'error' = 'success';

  showToast = false;

  // ───────────────── COLORS ─────────────────

  private cardColors = [
    '#c8e6c9',
    '#ffe0b2',
    '#b3d9ee',
    '#f8d7d7',
    '#e8d5f5',
    '#d5f0e8'
  ];

  constructor(
    private groupsService: GroupsService
  ) {}

  // ───────────────── INIT ─────────────────

  ngOnInit(): void {

    this.loadGroups();

    this.loadNotifications();

    this.loadInvitations();
  }

  ngOnDestroy(): void {

    this.pollSub?.unsubscribe();
  }

  // ───────────────── TOAST ─────────────────

  showToastMessage(
    message: string,
    type: 'success' | 'error' = 'success'
  ): void {

    this.toastMessage = message;

    this.toastType = type;

    this.showToast = true;

    setTimeout(() => {

      this.showToast = false;

    }, 3000);
  }

  // ───────────────── OWNER LOGIC ─────────────────

  isOwner(g: any): boolean {

    return (
      g?.ownerUsername ===
      this.currentUsername
    );
  }

  // ───────────────── GROUPS ─────────────────

  loadGroups(): void {

    this.loadingGroups = true;

    this.groupsService.getGroups().subscribe({

      next: data => {

        this.groups = data;

        this.loadingGroups = false;

        if (
          this.groups.length > 0 &&
          !this.selectedGroup
        ) {

          this.selectGroup(this.groups[0]);
        }
      },

      error: () => {

        this.loadingGroups = false;

        this.showToastMessage(
          'Erreur chargement groupes',
          'error'
        );
      }
    });
  }

  selectGroup(g: any): void {
    console.log('GROUP =>', g);

    this.selectedGroup = g;

    this.messages = [];

    this.loadMessages(g.id);

    this.startPolling(g.id);

    this.loadCommonSlots(g.id);

this.loadGroupSessions(g.id);
  }

  groupColor(i: number): string {

    return this.cardColors[
      i % this.cardColors.length
    ];
  }

  membersPreview(g: any): any[] {

    return (g.members ?? []).slice(0, 4);
  }

  extraMembers(g: any): number {

    return Math.max(
      0,
      (g.members ?? []).length - 4
    );
  }

  memberInitial(member: any): string {

    if (!member?.username) {
      return '?';
    }

    return member.username
      .charAt(0)
      .toUpperCase();
  }

  
  // ───────────────── CREATE GROUP ─────────────────

  openCreateModal(): void {

    this.showCreateModal = true;

    this.newGroupName = '';

    this.errorMsg = '';
  }

  closeCreateModal(): void {

    this.showCreateModal = false;
  }

  createGroup(): void {

    if (!this.newGroupName.trim()) {
      return;
    }

    this.creatingGroup = true;

    this.errorMsg = '';

    this.groupsService.createGroup({

      name: this.newGroupName.trim()

    }).subscribe({

      next: g => {

        this.groups.push(g);

        this.creatingGroup = false;

        this.showCreateModal = false;

        this.selectGroup(g);

        this.showToastMessage(
          'Groupe créé avec succès',
          'success'
        );
      },

      error: err => {

        this.errorMsg =
          err?.error?.message
          ?? 'Erreur création groupe';

        this.creatingGroup = false;

        this.showToastMessage(
          this.errorMsg,
          'error'
        );
      }
    });
  }

  // ───────────────── DELETE GROUP ─────────────────

  deleteGroup(
    g: any,
    event: Event
  ): void {

    event.stopPropagation();

    if (
      !confirm(
        `Supprimer "${g.name}" ?`
      )
    ) {
      return;
    }

    this.groupsService.deleteGroup(g.id)
      .subscribe({

        next: () => {

          this.groups =
            this.groups.filter(
              x => x.id !== g.id
            );

          if (
            this.selectedGroup?.id === g.id
          ) {

            this.selectedGroup = null;

            this.messages = [];

            this.pollSub?.unsubscribe();
          }

          this.showToastMessage(
            'Groupe supprimé',
            'success'
          );
        },

        error: err => {

          this.showToastMessage(
            err?.error?.message
              ?? 'Erreur suppression groupe',
            'error'
          );
        }
      });
  }

  // ───────────────── LEAVE GROUP ─────────────────

  leaveGroup(
    g: any,
    event: Event
  ): void {

    event.stopPropagation();

    this.groupsService.leaveGroup(g.id)
      .subscribe({

        next: () => {

          this.groups =
            this.groups.filter(
              x => x.id !== g.id
            );

          if (
            this.selectedGroup?.id === g.id
          ) {

            this.selectedGroup = null;

            this.messages = [];
          }

          this.showToastMessage(
            'Vous avez quitté le groupe',
            'success'
          );
        },

        error: err => {

          this.showToastMessage(
            err?.error?.message
              ?? 'Erreur quitter groupe',
            'error'
          );
        }
      });
  }

  // ───────────────── REMOVE MEMBER ─────────────────

  removeMember(memberId: string): void {

    if (!this.selectedGroup) {
      return;
    }

    this.groupsService.removeMember(
      this.selectedGroup.id,
      memberId
    ).subscribe({

      next: () => {

        this.selectedGroup.members =
          this.selectedGroup.members.filter(
            (m: any) => m.id !== memberId
          );

        this.showToastMessage(
          'Membre retiré avec succès',
          'success'
        );
      },

      error: err => {

        this.showToastMessage(
          err?.error?.message
            ?? 'Erreur suppression membre',
          'error'
        );
      }
    });
  }

  // ───────────────── INVITATIONS ─────────────────

  loadInvitations(): void {

    this.groupsService.getMyInvitations()
      .subscribe({

        next: inv => {

          this.invitations =
            inv.filter(
              (i: any) =>
                i.status === 'PENDING'
            );
        }
      });
  }

  openInviteModal(): void {

    this.showInviteModal = true;

    this.inviteReceiverId = '';

    this.errorMsg = '';
  }

  closeInviteModal(): void {

    this.showInviteModal = false;
  }

  sendInvite(): void {

    if (
      !this.inviteReceiverId.trim()
      ||
      !this.selectedGroup
    ) {
      return;
    }

    this.sendingInvite = true;

    this.errorMsg = '';

    this.groupsService.sendInvitation({

      groupId: this.selectedGroup.id,

      receiverId:
        this.inviteReceiverId.trim()

    }).subscribe({

      next: () => {

        this.showInviteModal = false;

        this.sendingInvite = false;

        this.showToastMessage(
          'Invitation envoyée',
          'success'
        );
      },

      error: err => {

        this.errorMsg =
          err?.error?.message
          ?? 'Erreur invitation';

        this.sendingInvite = false;

        this.showToastMessage(
          this.errorMsg,
          'error'
        );
      }
    });
  }

  acceptInvite(id: string): void {

    this.groupsService.acceptInvitation(id)
      .subscribe({

        next: () => {

          this.invitations =
            this.invitations.filter(
              i => i.id !== id
            );

          this.loadGroups();

          this.showToastMessage(
            'Invitation acceptée',
            'success'
          );
        }
      });
  }

  rejectInvite(id: string): void {

    this.groupsService.rejectInvitation(id)
      .subscribe({

        next: () => {

          this.invitations =
            this.invitations.filter(
              i => i.id !== id
            );

          this.showToastMessage(
            'Invitation refusée',
            'success'
          );
        }
      });
  }

  // ───────────────── CHAT ─────────────────

  loadMessages(groupId: string): void {

    this.groupsService
      .getGroupMessages(groupId)
      .subscribe({

        next: msgs => {

          this.messages = msgs;

          this.scrollToBottom();
        }
      });
  }

  startPolling(groupId: string): void {

    this.pollSub?.unsubscribe();

    this.pollSub = interval(4000)

      .pipe(

        switchMap(() =>
          this.groupsService
            .getGroupMessages(groupId)
        )
      )

      .subscribe({

        next: msgs => {

          this.messages = msgs;

          this.scrollToBottom();
        }
      });
  }

  sendMessage(): void {

    const content =
      this.newMessage.trim();

    if (
      !content
      ||
      !this.selectedGroup
      ||
      this.sendingMsg
    ) {
      return;
    }

    this.sendingMsg = true;

    this.groupsService.sendMessage(
      this.selectedGroup.id,
      content
    ).subscribe({

      next: msg => {

        this.messages.push(msg);

        this.newMessage = '';

        this.sendingMsg = false;

        this.scrollToBottom();
      },

      error: () => {

        this.sendingMsg = false;

        this.showToastMessage(
          'Erreur envoi message',
          'error'
        );
      }
    });
  }

  onEnter(event: KeyboardEvent): void {

    if (
      event.key === 'Enter'
      &&
      !event.shiftKey
    ) {

      event.preventDefault();

      this.sendMessage();
    }
  }

  private scrollToBottom(): void {

    setTimeout(() => {

      const el =
        document.getElementById(
          'chat-messages'
        );

      if (el) {

        el.scrollTop = el.scrollHeight;
      }

    }, 50);
  }

  // ───────────────── NOTIFICATIONS ─────────────────

  loadNotifications(): void {

    this.groupsService.getNotifications()
      .subscribe({

        next: n => {

          this.notifications =
            n.slice(0, 8);
        }
      });
  }

  markRead(id: string): void {

    this.groupsService.markAsRead(id)
      .subscribe({

        next: () => {

          const n =
            this.notifications.find(
              x => x.id === id
            );

          if (n) {

            n.isRead = true;
          }
        }
      });
  }

  notifColor(type: string): string {

    switch (type) {

      case 'GROUP_INVITATION':
        return '#c8e6c9';

      case 'OBJECTIVE_COMPLETED':
        return '#ffe0b2';

      case 'SESSION_REMINDER':
        return '#f8d7d7';

      default:
        return '#ece9e2';
    }
  }

  get unreadCount(): number {

    return this.notifications
      .filter(n => !n.isRead)
      .length;
  }

  // ───────────────── SESSIONS ─────────────────

  loadCommonSlots(groupId: string): void {

    this.loadingSlots = true;

    this.groupsService
      .getCommonAvailabilities(groupId)
      .subscribe({

        next: slots => {

          this.commonSlots = slots;

          this.loadingSlots = false;
        },

        error: () => {

          this.loadingSlots = false;
        }
      });
  }

  openSessionModal(slot: any): void {

    this.sessionDay = slot.day;

    this.sessionStartTime =
      slot.startTime;

    this.sessionEndTime =
      slot.endTime;

    this.sessionSubjectId = '';

    this.sessionError = '';

    this.groupsService
      .getMySubjects()
      .subscribe({

        next: data => {

          this.subjects = data;
        }
      });

    this.showSessionModal = true;
  }

  closeSessionModal(): void {

    this.showSessionModal = false;
  }

  createSession(): void {

    if (
      !this.sessionSubjectId.trim()
      ||
      !this.selectedGroup
    ) {
      return;
    }

    this.creatingSession = true;

    this.sessionError = '';

    const today = new Date();

    const dayMap:
      Record<string, number> = {

      MONDAY: 1,
      TUESDAY: 2,
      WEDNESDAY: 3,
      THURSDAY: 4,
      FRIDAY: 5,
      SATURDAY: 6,
      SUNDAY: 0
    };

    const targetDay =
      dayMap[this.sessionDay] ?? 1;

    const currentDay =
      today.getDay();

    let diff =
      targetDay - currentDay;

    if (diff < 0) {

      diff += 7;
    }

    const sessionDate =
      new Date(today);

    sessionDate.setDate(
      today.getDate() + diff
    );

    const dateStr =
      sessionDate
        .toISOString()
        .split('T')[0];

    const dto = {

      groupId:
        this.selectedGroup.id,

      subjectId:
        this.sessionSubjectId.trim(),

      startTime:
        `${dateStr}T${this.sessionStartTime}:00`,

      endTime:
        `${dateStr}T${this.sessionEndTime}:00`
    };

    this.groupsService
      .createCollaborativeSession(dto)
      .subscribe({

        next: () => {

          this.showSessionModal = false;

          this.creatingSession = false;

          this.showToastMessage(
            'Session créée avec succès',
            'success'
          );
        },

        error: err => {

          this.sessionError =
            err?.error?.message
            ?? 'Erreur création session';

          this.creatingSession = false;

          this.showToastMessage(
            this.sessionError,
            'error'
          );
        }
      });
  }
  // ───────────────── GROUP SESSIONS ─────────────────

loadGroupSessions(groupId: string): void {

  this.loadingSessions = true;

  this.groupsService
    .getGroupSessions(groupId)
    .subscribe({

      next: sessions => {
        console.log('SESSIONS =>', sessions);

        this.groupSessions = sessions;

        this.loadingSessions = false;
      },

      error: err => {

        this.loadingSessions = false;

        this.showToastMessage(
          err?.error?.message
          ?? 'Erreur chargement sessions',
          'error'
        );
      }
    });
}

startSession(session: any): void {

  this.groupsService
    .startSession(session.id)
    .subscribe({

      next: updated => {

        session.status = updated.status;

        this.showToastMessage(
          'Session démarrée',
          'success'
        );
      },

      error: err => {

        const msg =
          err?.error?.message
          || err?.error
          || '';

        if (
          msg.includes(
            'before start time'
          )
        ) {

          this.showToastMessage(
            'Vous ne pouvez pas démarrer une session avant son heure.',
            'error'
          );

          return;
        }

        if (
          msg.includes(
            'already expired'
          )
        ) {

          this.showToastMessage(
            'Cette session est expirée.',
            'error'
          );

          return;
        }

        if (
          msg.includes(
            'cannot be started'
          )
        ) {

          this.showToastMessage(
            'Cette session ne peut pas être démarrée.',
            'error'
          );

          return;
        }

        this.showToastMessage(
          'Erreur démarrage session',
          'error'
        );
      }
    });
}

completeSession(session: any): void {

  this.groupsService
    .completeSession(session.id)
    .subscribe({

      next: updated => {

        session.status = updated.status;

        this.showToastMessage(
          'Session terminée',
          'success'
        );
      },

      error: err => {

        const msg =
          err?.error?.message
          || err?.error
          || '';

        if (
          msg.includes(
            'must be ongoing'
          )
        ) {

          this.showToastMessage(
            'La session doit être en cours pour être terminée.',
            'error'
          );

          return;
        }

        this.showToastMessage(
          'Impossible de terminer la session.',
          'error'
        );
      }
    });
}

cancelSession(session: any): void {

  this.groupsService
    .cancelSession(session.id)
    .subscribe({

      next: updated => {

        session.status = updated.status;

        this.showToastMessage(
          'Session annulée',
          'success'
        );
      },

      error: err => {

        const msg =
          err?.error?.message
          || err?.error
          || '';

        if (
          msg.includes(
            'cannot be cancelled'
          )
        ) {

          this.showToastMessage(
            'Une session terminée ne peut pas être annulée.',
            'error'
          );

          return;
        }

        this.showToastMessage(
          'Impossible d’annuler la session.',
          'error'
        );
      }
    });
}
deleteSession(session: any): void {

  if (!confirm(
    'Supprimer cette session ?'
  )) {
    return;
  }

  this.groupsService
    .deleteSession(session.id)
    .subscribe({

      next: () => {

        this.groupSessions =
          this.groupSessions.filter(
            s => s.id !== session.id
          );

        this.showToastMessage(
          'Session supprimée',
          'success'
        );
      },

      error: err => {

        const msg =
          err?.error?.message
          || err?.error
          || '';

        if (
          msg.includes(
            'Ongoing session cannot be deleted'
          )
        ) {

          this.showToastMessage(
            'Une session en cours ne peut pas être supprimée.',
            'error'
          );

          return;
        }

        this.showToastMessage(
          'Impossible de supprimer la session.',
          'error'
        );
      }
    });
}

shareSession(session: any): void {

  this.groupsService
    .shareSessionToGroup(
      session.id,
      this.selectedGroup.id
    )
    .subscribe({

      next: () => {

        this.showToastMessage(
          'Session partagée avec le groupe.',
          'success'
        );

        // IMPORTANT
        this.loadGroupSessions(
          this.selectedGroup.id
        );
      },

      error: err => {

        console.log(err);

        this.showToastMessage(
          'Impossible de partager la session.',
          'error'
        );
      }
    });
}

isSessionOwner(session: any): boolean {

  console.log(
    'SESSION USER =',
    session.userUsername
  );

  console.log(
    'CURRENT USER =',
    this.currentUsername
  );

  return (
    session.userUsername ===
    this.currentUsername
  );
}

statusClass(status: string): string {

  switch (status) {

    case 'PLANNED':
      return 'status-planned';

    case 'ONGOING':
      return 'status-ongoing';

    case 'DONE':
      return 'status-done';

    case 'CANCELLED':
      return 'status-cancelled';

    default:
      return '';
  }
}

  // ───────────────── FORMAT ─────────────────

  formatTime(dateStr: string): string {

    if (!dateStr) {
      return '';
    }

    return new Date(dateStr)
      .toLocaleTimeString(
        'fr-FR',
        {
          hour: '2-digit',
          minute: '2-digit'
        }
      );
  }

  formatDate(dateStr: string): string {

    if (!dateStr) {
      return '';
    }

    return new Date(dateStr)
      .toLocaleDateString(
        'fr-FR',
        {
          weekday: 'short',
          day: 'numeric',
          month: 'short'
        }
      );
  }

  dayFr(day: string): string {

    const map:
      Record<string, string> = {

      MONDAY: 'Lundi',
      TUESDAY: 'Mardi',
      WEDNESDAY: 'Mercredi',
      THURSDAY: 'Jeudi',
      FRIDAY: 'Vendredi',
      SATURDAY: 'Samedi',
      SUNDAY: 'Dimanche'
    };

    return map[day] ?? day;
  }
}