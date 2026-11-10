import type { PageResponse } from "./types";

export interface NotificationItem {
  id: number;
  title: string;
  body: string;
  kind: string | null;
  link: string | null;
  read: boolean;
  createdAt: string;
}

export interface UnreadCount {
  count: number;
}

export type NotificationPage = PageResponse<NotificationItem>;
