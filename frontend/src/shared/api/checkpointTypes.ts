import type { GroupRef, SubmissionStatus } from "./types";

export interface CheckpointSummary {
  id: number;
  title: string;
  description: string;
  deadline: string;
  cost: number;
  groups: GroupRef[];
  createdAt: string;
  teacherName: string;
  studentsCount: number;
  submittedCount: number;
  acceptedCount: number;
  overdueCount: number;
}

export interface RosterRow {
  studentUserId: number;
  studentProfileId: number;
  fullName: string;
  groupName: string;
  status: SubmissionStatus;
  arDebited: number;
  cost: number;
  submittedAt: string | null;
  submissionUrl: string | null;
  submissionId: number;
  fileName: string | null;
  returnComment: string | null;
}

export interface StudentCheckpoint {
  submissionId: number;
  checkpointId: number;
  title: string;
  description: string;
  deadline: string;
  cost: number;
  status: SubmissionStatus;
  arDebited: number;
  submittedAt: string | null;
  submissionUrl: string | null;
  fileName: string | null;
  returnComment: string | null;
  teacherName: string;
}

export interface CheckpointDetail {
  id: number;
  title: string;
  description: string;
  deadline: string;
  cost: number;
  groups: GroupRef[];
  createdAt: string;
  teacherName: string;
  studentsCount: number | null;
  submittedCount: number | null;
  acceptedCount: number | null;
  overdueCount: number | null;
  roster: RosterRow[] | null;
  mySubmission: StudentCheckpoint | null;
}
