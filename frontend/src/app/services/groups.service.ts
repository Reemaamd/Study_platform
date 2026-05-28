import { Injectable } from '@angular/core';

import { HttpClient } from '@angular/common/http';

import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class GroupsService {

  private readonly groupApi =
    'http://localhost:8080/groups';

  private readonly inviteApi =
    'http://localhost:8080/invitations';

  private readonly msgApi =
    'http://localhost:8080/messages';

  private readonly notifApi =
    'http://localhost:8080/notifications';

  private readonly colabApi =
    'http://localhost:8080/collaborative-sessions';

  private readonly subjectApi =
    'http://localhost:8080/subjects';

  constructor(
    private http: HttpClient
  ) {}

  // ───────────────── GROUPS ─────────────────

  getGroups(): Observable<any[]> {

    return this.http.get<any[]>(
      this.groupApi
    );
  }

  createGroup(
    dto: { name: string }
  ): Observable<any> {

    return this.http.post<any>(
      this.groupApi,
      dto
    );
  }

  deleteGroup(
    groupId: string
  ): Observable<any> {

    return this.http.delete<any>(
      `${this.groupApi}/${groupId}`
    );
  }

  leaveGroup(
    groupId: string
  ): Observable<any> {

    return this.http.post<any>(
      `${this.groupApi}/${groupId}/leave`,
      {}
    );
  }

  removeMember(
    groupId: string,
    memberId: string
  ): Observable<any> {

    return this.http.delete<any>(
      `${this.groupApi}/${groupId}/members/${memberId}`
    );
  }

  // ───────────────── SUBJECTS ─────────────────

  getMySubjects(): Observable<any[]> {

    return this.http.get<any[]>(
      this.subjectApi
    );
  }

  // ───────────────── INVITATIONS ─────────────────

  sendInvitation(dto: {
    groupId: string;
    receiverId: string;
  }): Observable<any> {

    return this.http.post<any>(
      this.inviteApi,
      dto
    );
  }

  getMyInvitations(): Observable<any[]> {

    return this.http.get<any[]>(
      this.inviteApi
    );
  }

  acceptInvitation(
    id: string
  ): Observable<any> {

    return this.http.post<any>(
      `${this.inviteApi}/${id}/accept`,
      {}
    );
  }

  rejectInvitation(
    id: string
  ): Observable<any> {

    return this.http.post<any>(
      `${this.inviteApi}/${id}/reject`,
      {}
    );
  }

  // ───────────────── MESSAGES ─────────────────

  sendMessage(
    groupId: string,
    content: string
  ): Observable<any> {

    return this.http.post<any>(
      `${this.msgApi}/${groupId}`,
      { content }
    );
  }

  getGroupMessages(
    groupId: string
  ): Observable<any[]> {

    return this.http.get<any[]>(
      `${this.msgApi}/${groupId}`
    );
  }

  // ───────────────── NOTIFICATIONS ─────────────────

  getNotifications(): Observable<any[]> {

    return this.http.get<any[]>(
      this.notifApi
    );
  }

  markAsRead(
    id: string
  ): Observable<any> {

    return this.http.put<any>(
      `${this.notifApi}/${id}/read`,
      {}
    );
  }

  // ───────────────── COMMON AVAILABILITIES ─────────────────

  getCommonAvailabilities(
    groupId: string
  ): Observable<any[]> {

    return this.http.get<any[]>(
      `${this.colabApi}/${groupId}/common-availabilities`
    );
  }

  // ───────────────── CREATE SESSION ─────────────────

  createCollaborativeSession(dto: {

    groupId: string;

    subjectId: string;

    startTime: string;

    endTime: string;

  }): Observable<any> {

    return this.http.post<any>(
      this.colabApi,
      dto
    );
  }

  // ───────────────── GROUP SESSIONS ─────────────────

  getGroupSessions(
    groupId: string
  ): Observable<any[]> {

    return this.http.get<any[]>(
      `${this.colabApi}/group/${groupId}`
    );
  }

  // ───────────────── SESSION ACTIONS ─────────────────

  startSession(
    id: string
  ): Observable<any> {

    return this.http.patch<any>(
      `${this.colabApi}/${id}/start`,
      {}
    );
  }

  completeSession(
    id: string
  ): Observable<any> {

    return this.http.patch<any>(
      `${this.colabApi}/${id}/complete`,
      {}
    );
  }

  cancelSession(
    id: string
  ): Observable<any> {

    return this.http.patch<any>(
      `${this.colabApi}/${id}/cancel`,
      {}
    );
  }

  deleteSession(
    id: string
  ): Observable<any> {

    return this.http.delete<any>(
      `${this.colabApi}/${id}`
    );
  }

  // ───────────────── SHARE SESSION ─────────────────

  shareSessionToGroup(
    sessionId: string,
    groupId: string
  ): Observable<any> {

    return this.http.post<any>(
      `${this.colabApi}/${sessionId}/share/${groupId}`,
      {}
    );
  }

}