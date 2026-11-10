import type { GroupRef } from "./types";

export type PollQuestionType = "SINGLE" | "MULTIPLE";
export type PollStatus = "ACTIVE" | "ENDED";

export interface PollSummary {
  id: number;
  title: string;
  endsAt: string;
  isAnonymous: boolean;
  status: PollStatus;
  hasVoted: boolean;
  questionCount: number;
  createdAt: string;
  targetGroups: GroupRef[];
}

export interface PollOption {
  id: number;
  text: string;
}

export interface PollQuestion {
  id: number;
  text: string;
  type: PollQuestionType;
  options: PollOption[];
}

export interface PollOptionResult {
  optionId: number;
  text: string;
  count: number;
  percent: number;
}

export interface PollQuestionResult {
  questionId: number;
  text: string;
  type: PollQuestionType;
  totalVotes: number;
  options: PollOptionResult[];
}

export interface PollVoterAnswer {
  questionId: number;
  options: string[];
}

export interface PollVoter {
  studentName: string;
  groupName: string;
  answers: PollVoterAnswer[];
}

export interface PollDetail {
  id: number;
  title: string;
  description: string;
  endsAt: string;
  isAnonymous: boolean;
  status: PollStatus;
  hasVoted: boolean;
  canVote: boolean;
  targetGroups: GroupRef[];
  createdAt: string;
  teacherName: string;
  questions: PollQuestion[];
  results: PollQuestionResult[] | null;
  voters: PollVoter[];
}

export interface PollResults {
  pollId: number;
  title: string;
  status: PollStatus;
  isAnonymous: boolean;
  totalResponses: number;
  results: PollQuestionResult[];
  voters: PollVoter[];
}

export interface VoteAnswer {
  questionId: number;
  optionIds: number[];
}
