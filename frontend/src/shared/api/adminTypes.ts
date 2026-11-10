import type { MembershipStatus, Role } from "./types";

export interface AdminStats {
  faculties: number;
  groups: number;
  departments: number;
  students: number;
  teachers: number;
  admins: number;
  pendingMemberships: number;
  checkpoints: number;
  activities: number;
  polls: number;
  awards: number;
}

export interface FacultyAdmin {
  id: number;
  name: string;
  groupsCount: number;
}

export interface GroupAdmin {
  id: number;
  name: string;
  facultyId: number;
  facultyName: string;
}

export interface DepartmentAdmin {
  id: number;
  name: string;
  teachersCount: number;
}

export interface Setting {
  key: string;
  value: string;
  title: string;
  description: string;
  min: number;
  max: number;
  unit: string;
}

export interface Semester {
  id: number;
  name: string;
  startAt: string;
  endAt: string;
  active: boolean;
  activatedAt: string | null;
}

export interface SemesterActivation {
  semester: Semester;
  affectedStudents: number;
  totalBonus: number;
}

export interface ImportError {
  row: number;
  message: string;
}

export interface ImportReport {
  created: number;
  skipped: number;
  errors: ImportError[];
}

export interface AdminUser {
  id: number;
  email: string;
  fullName: string;
  role: Role;
  emailVerified: boolean;
  groupName: string | null;
  departmentName: string | null;
  membershipStatus: MembershipStatus | null;
  createdAt: string;
}
