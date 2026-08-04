export type UserRole = 'OWNER' | 'ASSIGNEE';
export type Category = 'BUG' | 'FEATURE_REQUEST' | 'SUPPORT' | 'BILLING' | 'OTHER';
export type Urgency = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type TaskStatus =
  | 'SUBMITTED'
  | 'TRIAGED'
  | 'NEEDS_REVIEW'
  | 'QUEUED'
  | 'IN_PROGRESS'
  | 'DONE'
  | 'ARCHIVED';
export type TaskSource = 'EMAIL' | 'MESSAGE' | 'MANUAL';
export type TriageGate = 'RULE_BASED' | 'LLM';

export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
}

export interface UserResponse {
  id: number;
  name: string;
  email: string;
  role: UserRole;
}

export interface TaskResponse {
  id: number;
  title: string;
  description: string | null;
  category: Category | null;
  urgency: Urgency | null;
  status: TaskStatus;
  source: TaskSource;
  dueAt: string | null;
  ownerId: number;
  createdAt: string;
  updatedAt: string;
}

export interface TaskStatusHistoryEntry {
  fromStatus: TaskStatus | null;
  toStatus: TaskStatus;
  changedAt: string;
}

export interface TaskDetailResponse extends TaskResponse {
  statusHistory: TaskStatusHistoryEntry[];
}

export interface TaskListResponse {
  items: TaskResponse[];
  page: number;
  size: number;
  totalElements: number;
}

export interface TriageLogResponse {
  id: number;
  gate: TriageGate;
  llmCalled: boolean;
  confidenceScore: number | null;
  resultCategory: Category | null;
  resultUrgency: Urgency | null;
  createdAt: string;
}

export interface DashboardSummaryResponse {
  countsByStatus: Record<string, number>;
  slaApproaching: number;
  slaBreached: number;
}

export interface AdminMetricsResponse {
  gate1FilteredRate: number;
  gate2CallRate: number;
  avgConfidenceScore: number;
  estimatedCostSavingUsd: number;
}

export interface ApiErrorBody {
  status: number;
  code: string;
  message: string;
  timestamp: string;
}
