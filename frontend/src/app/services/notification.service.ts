import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AppNotification {
  id: string;
  message: string;
  type: string;
  isRead: boolean;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {

  private readonly API = 'http://localhost:8080';

  private get headers(): HttpHeaders {
    const token = localStorage.getItem('token') || '';
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  constructor(private http: HttpClient) {}

  // GET /notifications
  getMyNotifications(): Observable<AppNotification[]> {
    return this.http.get<AppNotification[]>(
      `${this.API}/notifications`,
      { headers: this.headers }
    );
  }

  // PUT /notifications/{id}/read  ← endpoint exact du backend
  markAsRead(id: string): Observable<void> {
    return this.http.put<void>(
      `${this.API}/notifications/${id}/read`,
      {},
      { headers: this.headers }
    );
  }
  // PUT /notifications/mark-all-read
markAllAsRead(username: string): Observable<void> {
  return this.http.put<void>(
    `${this.API}/notifications/mark-all-read?username=${username}`,
    {},
    { headers: this.headers }
  );
}

  // GET /notifications/unread-count
  getUnreadCount(): Observable<number> {
    return this.http.get<number>(
      `${this.API}/notifications/unread-count`,
      { headers: this.headers }
    );
  }
}