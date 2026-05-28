// ── Groupes ────────────────────────────────────────────────

export interface GroupDTO {
  name: string;
}

export interface MemberDTO {
  id: string;
  username: string;
  owner: boolean;
}

export interface GroupResponseDTO {
  id: string;
  name: string;
  ownerId: string;
  members: MemberDTO[];
  createdAt: string;
}
// ── Invitations ────────────────────────────────────────────

export interface InvitationDTO {
  groupId: string;
  receiverId: string;
}

export interface InvitationResponseDTO {
  id: string;
  senderId: string;
  receiverId: string;
  groupId: string;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  createdAt: string;
}

// ── Messages ───────────────────────────────────────────────

export interface MessageResponseDTO {
  id: string;
  groupId: string;
  senderId: string;
  senderUsername: string;
  content: string;
  createdAt: string;
}

// ── Notifications ──────────────────────────────────────────

export interface NotificationDTO {
  id: string;
  userId: string;
  message: string;
  type: string;
  isRead: boolean;
  createdAt: string;
}
