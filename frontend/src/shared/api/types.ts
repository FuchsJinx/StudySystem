export type Role = "STUDENT" | "TEACHER" | "ADMIN";

export type MembershipStatus = "PENDING" | "CONFIRMED" | "REJECTED";

export type SubmissionStatus =
  | "IN_PROGRESS"
  | "SUBMITTED"
  | "OVERDUE"
  | "ACCEPTED"
  | "RETURNED";

export type PointCurrency = "AR" | "BUA" | "BOA";

export interface GroupRef {
  id: number;
  name: string;
}

export interface StudentInfo {
  id: number;
  groupId: number;
  groupName: string;
  facultyId: number;
  facultyName: string;
  membershipStatus: MembershipStatus;
  isGroupLead: boolean;
  academicRating: number;
  bua: number;
  boa: number;
}

export interface TeacherInfo {
  id: number;
  departmentId: number;
  departmentName: string;
  groups: GroupRef[];
}

export interface UserMe {
  id: number;
  email: string;
  fullName: string;
  role: Role;
  emailVerified: boolean;
  createdAt: string;
  student: StudentInfo | null;
  teacher: TeacherInfo | null;
}

export interface AuthResponse {
  token: string;
  user: UserMe;
}

export interface Faculty {
  id: number;
  name: string;
}

export interface Group {
  id: number;
  name: string;
  facultyId: number;
  facultyName: string;
}

export interface Department {
  id: number;
  name: string;
}

export interface LedgerEntry {
  id: number;
  date: string;
  currency: PointCurrency;
  amount: number;
  reason: string;
  actorName: string | null;
  refType: string | null;
  refId: number | null;
}

export interface ProfileCheckpoint {
  id: number;
  title: string;
  deadline: string;
  status: SubmissionStatus;
  cost: number;
  arDebited: number;
}

export interface ProfileResponse {
  id: number;
  fullName: string;
  role: Role;
  email: string | null;
  emailVerified: boolean;
  groupName: string | null;
  facultyName: string | null;
  departmentName: string | null;
  academicRating: number | null;
  bua: number | null;
  boa: number | null;
  membershipStatus: MembershipStatus | null;
  isGroupLead: boolean | null;
  ledger: LedgerEntry[];
  activeCheckpoints: ProfileCheckpoint[];
  completedCheckpoints: ProfileCheckpoint[];
}

export interface MembershipRequest {
  studentUserId: number;
  studentProfileId: number;
  fullName: string;
  email: string;
  groupId: number;
  groupName: string;
  facultyName: string;
  status: MembershipStatus;
  requestedAt: string;
}

export interface NotificationSettings {
  emailEnabled: boolean;
  telegramEnabled: boolean;
  telegramChatId: string | null;
}

export interface MessageResponse {
  message: string;
}
