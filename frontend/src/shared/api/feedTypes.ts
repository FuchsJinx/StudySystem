export type PersonalEventType =
  | "CHECKPOINT_ASSIGNED"
  | "DEADLINE_SOON"
  | "SUBMISSION_ACCEPTED"
  | "SUBMISSION_RETURNED"
  | "POINTS_AWARDED"
  | "POLL_CREATED"
  | "ACTIVITY_CREATED"
  | "SUBMISSION_RECEIVED"
  | "POLL_ENDED"
  | "MEMBERSHIP_CONFIRMED"
  | "MEMBERSHIP_REJECTED"
  | "SEMESTER_BONUS";

export interface PersonalFeedItem {
  id: number;
  type: PersonalEventType;
  message: string;
  createdAt: string;
}
