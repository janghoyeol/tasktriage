import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { type FormEvent, useState } from 'react';
import { Link } from 'react-router-dom';
import { ApiError, api } from '../api/client';
import type { TaskListResponse, TaskResponse, TaskSource, TaskStatus, Urgency } from '../api/types';

const STATUS_OPTIONS: TaskStatus[] = [
  'SUBMITTED',
  'TRIAGED',
  'NEEDS_REVIEW',
  'QUEUED',
  'IN_PROGRESS',
  'DONE',
  'ARCHIVED',
];
const URGENCY_OPTIONS: Urgency[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];
const SOURCE_OPTIONS: TaskSource[] = ['EMAIL', 'MESSAGE', 'MANUAL'];

export default function TaskListPage() {
  const queryClient = useQueryClient();
  const [statusFilter, setStatusFilter] = useState<TaskStatus | ''>('');
  const [urgencyFilter, setUrgencyFilter] = useState<Urgency | ''>('');

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [source, setSource] = useState<TaskSource>('MANUAL');
  const [createError, setCreateError] = useState<string | null>(null);

  const params = new URLSearchParams();
  if (statusFilter) params.set('status', statusFilter);
  if (urgencyFilter) params.set('urgency', urgencyFilter);
  params.set('size', '50');

  const { data, isLoading, isError } = useQuery({
    queryKey: ['tasks', statusFilter, urgencyFilter],
    queryFn: () => api.get<TaskListResponse>(`/api/tasks?${params.toString()}`),
  });

  const createTaskMutation = useMutation({
    mutationFn: () =>
      api.post<TaskResponse>('/api/tasks', { title, description: description || null, source }),
    onSuccess: () => {
      setTitle('');
      setDescription('');
      void queryClient.invalidateQueries({ queryKey: ['tasks'] });
    },
    onError: (err: unknown) => {
      setCreateError(err instanceof ApiError ? err.message : '작업 등록에 실패했습니다.');
    },
  });

  function handleCreate(event: FormEvent) {
    event.preventDefault();
    setCreateError(null);
    createTaskMutation.mutate();
  }

  return (
    <div>
      <h1>작업 목록</h1>

      <form onSubmit={handleCreate} className="create-task-form">
        <input type="text" placeholder="제목" value={title} onChange={(e) => setTitle(e.target.value)} required />
        <input
          type="text"
          placeholder="설명 (선택)"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
        <select value={source} onChange={(e) => setSource(e.target.value as TaskSource)}>
          {SOURCE_OPTIONS.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
        <button type="submit" className="primary" disabled={createTaskMutation.isPending}>
          {createTaskMutation.isPending ? '등록 중...' : '작업 등록'}
        </button>
      </form>
      {createError && <p className="form-error">{createError}</p>}

      <div className="filters">
        <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value as TaskStatus | '')}>
          <option value="">전체 상태</option>
          {STATUS_OPTIONS.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
        <select value={urgencyFilter} onChange={(e) => setUrgencyFilter(e.target.value as Urgency | '')}>
          <option value="">전체 긴급도</option>
          {URGENCY_OPTIONS.map((u) => (
            <option key={u} value={u}>
              {u}
            </option>
          ))}
        </select>
      </div>

      {isLoading && <p>불러오는 중...</p>}
      {isError && <p className="form-error">작업 목록을 불러오지 못했습니다.</p>}

      {data && data.items.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>제목</th>
              <th>카테고리</th>
              <th>긴급도</th>
              <th>상태</th>
              <th>마감</th>
            </tr>
          </thead>
          <tbody>
            {data.items.map((task) => (
              <tr key={task.id}>
                <td>
                  <Link to={`/tasks/${task.id}`}>{task.title}</Link>
                </td>
                <td>{task.category ?? '-'}</td>
                <td>{task.urgency ?? '-'}</td>
                <td>
                  <span className="badge">{task.status}</span>
                </td>
                <td>{task.dueAt ? new Date(task.dueAt).toLocaleString() : '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {data && data.items.length === 0 && <p>등록된 작업이 없습니다.</p>}
    </div>
  );
}
