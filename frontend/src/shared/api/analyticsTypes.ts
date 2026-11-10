import type { GroupBudget } from "./activityTypes";

export interface ArPoint {
  date: string;
  average: number;
  median: number;
}

export interface StudentAr {
  studentUserId: number;
  fullName: string;
  academicRating: number;
}

export interface ArDynamics {
  groupId: number;
  groupName: string;
  points: ArPoint[];
  students: StudentAr[];
}

export interface RankRow {
  studentUserId: number;
  fullName: string;
  groupName: string;
  bua: number;
  boa: number;
  total: number;
}

export interface ActivityRanking {
  groupId: number;
  groupName: string;
  students: RankRow[];
}

export interface GroupSpend {
  groupId: number;
  groupName: string;
  limit: number;
  spent: number;
}

export interface MonthBudget {
  yearMonth: string;
  limit: number;
  spent: number;
  groups: GroupSpend[];
}

export interface BudgetStats {
  months: MonthBudget[];
}

export interface TeacherOverview {
  groups: number;
  students: number;
  checkpoints: number;
  activities: number;
  polls: number;
  pendingMemberships: number;
  yearMonth: string;
  currentBudget: GroupBudget[];
}
