import type { GroupRef, PointCurrency } from "./types";

export type ActivityType = "ACADEMIC" | "SOCIAL";

export interface ActivitySummary {
  id: number;
  title: string;
  description: string;
  type: ActivityType;
  eventDate: string;
  maxPoints: number;
  groups: GroupRef[];
  createdAt: string;
  teacherName: string;
  enrolledCount: number;
  awardedCount: number;
  enrolled: boolean | null;
  myAwardedPoints: number | null;
  myAwardedLevel: number | null;
}

export interface Participant {
  studentUserId: number;
  studentProfileId: number;
  fullName: string;
  groupName: string;
  enrolled: boolean;
  awardedPoints: number | null;
  awardedLevel: number | null;
}

export interface AwardResult {
  studentUserId: number;
  studentName: string;
  points: number;
  currency: PointCurrency;
  remainingBudget: number;
}

export interface AwardResponse {
  yearMonth: string;
  budgetLimit: number;
  awarded: AwardResult[];
}

export interface BudgetAward {
  awardId: number;
  studentName: string;
  activityTitle: string;
  points: number;
  currency: PointCurrency;
  date: string;
}

export interface BudgetDetail {
  yearMonth: string;
  groupId: number;
  groupName: string;
  limit: number;
  spent: number;
  remaining: number;
  awards: BudgetAward[];
}

export interface GroupBudget {
  groupId: number;
  groupName: string;
  limit: number;
  spent: number;
  remaining: number;
}

export interface BudgetOverview {
  yearMonth: string;
  totalLimit: number;
  totalSpent: number;
  totalRemaining: number;
  groups: GroupBudget[];
}

export interface AwardFeedItem {
  id: number;
  studentUserId: number;
  studentName: string;
  groupName: string;
  currency: PointCurrency;
  amount: number;
  activityTitle: string;
  teacherName: string;
  createdAt: string;
}
