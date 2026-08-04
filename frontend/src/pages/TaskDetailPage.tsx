import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { ApiError, api } from '../api/client';
import type { TaskDetailResponse, TaskStatus, TriageLogResponse } from '../api/types';

const ALL_STATUSES: TaskStatus[] = [
  'SUBMITTED',
  'TRIAGED',
  'NEEDS_REVIEW',
  'QUEUED',
  'IN_PROGRESS',
  'DONE',
  'ARCHIVED',
];

export default function TaskDetailPage() {
  const { id } = useParams<{ id: string }>();
  const queryClient = useQueryClient();
  const [statusError, setStatusError] = useState<string | null>(null);
  const [nextStatus, setNextStatus] = useState<TaskStatus>('SUBMITTED');

  const taskQuery = useQuery({
    queryKey: ['task', id],
    queryFn: () => api.get<TaskDetailResponse>(`/api/tasks/${id}`),
  });

  const triageLogQuery = useQuery({
    queryKey: ['task', id, 'triage-log'],
    queryFn: () => api.get<TriageLogResponse[]>(`/api/tasks/${id}/triage-log`),
  });

  const statusMutation = useMutation({
    mutationFn: (status: TaskStatus) => api.patch(`/api/tasks/${id}/status`, { status }),
    onSuccess: () => {
      setStatusError(null);
      void queryClient.invalidateQueries({ queryKey: ['task', id] });
      void queryClient.invalidateQueries({ queryKey: ['tasks'] });
    },
    onError: (err: unknown) => {
      setStatusError(err instanceof ApiError ? err.message : '상태 변경에 실패했습니다.');
    },
  });

  if (taskQuery.isLoading) {
    return <p>불러오는 중...</p>;
  }
  if (taskQuery.isError || !taskQuery.data) {
    return <p className="form-error">작업을 찾을 수 없습니다.</p>;
  }

  const task = taskQuery.data;

  return (
    <div>
      <h1>{task.title}</h1>
      <p>{task.description ?? '설명 없음'}</p>

      <div className="card-grid">
        <div className="card">
          <div className="card__label">상태</div>
          <div className="card__value">
            <span className="badge">{task.status}</span>
          </div>
        </div>
        <div className="card">
          <div className="card__label">카테고리</div>
          <div className="card__value">{task.category ?? '-'}</div>
        </div>
        <div className="card">
          <div className="card__label">긴급도</div>
          <div className="card__value">{task.urgency ?? '-'}</div>
        </div>
        <div className="card">
          <div className="card__label">마감</div>
          <div className="card__value">{task.dueAt ? new Date(task.dueAt).toLocaleString() : '-'}</div>
        </div>
      </div>

      <section>
        <h2>상태 변경</h2>
        <div className="filters">
          <select value={nextStatus} onChange={(e) => setNextStatus(e.target.value as TaskStatus)}>
            {ALL_STATUSES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
          <button
            type="button"
            className="primary"
            onClick={() => statusMutation.mutate(nextStatus)}
            disabled={statusMutation.isPending}
          >
            변경
          </button>
        </div>
        {statusError && <p className="form-error">{statusError}</p>}
      </section>

      <section>
        <h2>상태 이력</h2>
        <table>
          <thead>
            <tr>
              <th>From</th>
              <th>To</th>
              <th>시각</th>
            </tr>
          </thead>
          <tbody>
            {task.statusHistory.map((entry, idx) => (
              <tr key={idx}>
                <td>{entry.fromStatus ?? '-'}</td>
                <td>{entry.toStatus}</td>
                <td>{new Date(entry.changedAt).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      <section>
        <h2>분류 근거 (Triage Log)</h2>
        {triageLogQuery.data && triageLogQuery.data.length === 0 && <p>아직 분류되지 않았습니다.</p>}
        {triageLogQuery.data && triageLogQuery.data.length > 0 && (
          <table>
            <thead>
              <tr>
                <th>Gate</th>
                <th>LLM 호출</th>
                <th>Confidence</th>
                <th>결과</th>
                <th>시각</th>
              </tr>
            </thead>
            <tbody>
              {triageLogQuery.data.map((log) => (
                <tr key={log.id}>
                  <td>{log.gate}</td>
                  <td>{log.llmCalled ? 'Y' : 'N'}</td>
                  <td>{log.confidenceScore != null ? log.confidenceScore.toFixed(2) : '-'}</td>
                  <td>
                    {log.resultCategory} / {log.resultUrgency}
                  </td>
                  <td>{new Date(log.createdAt).toLocaleString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}
