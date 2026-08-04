import { useQuery } from '@tanstack/react-query';
import { api } from '../api/client';
import type { AdminMetricsResponse, DashboardSummaryResponse } from '../api/types';

export default function DashboardPage() {
  const summaryQuery = useQuery({
    queryKey: ['dashboard', 'summary'],
    queryFn: () => api.get<DashboardSummaryResponse>('/api/dashboard/summary'),
  });

  const metricsQuery = useQuery({
    queryKey: ['admin', 'metrics'],
    queryFn: () => api.get<AdminMetricsResponse>('/api/admin/metrics'),
  });

  return (
    <div>
      <h1>대시보드</h1>

      <section>
        <h2>SLA 현황</h2>
        {summaryQuery.data && (
          <div className="card-grid">
            <div className="card">
              <div className="card__label">임박 (24시간 이내)</div>
              <div className="card__value">{summaryQuery.data.slaApproaching}</div>
            </div>
            <div className="card">
              <div className="card__label">위반</div>
              <div className="card__value">{summaryQuery.data.slaBreached}</div>
            </div>
          </div>
        )}
      </section>

      <section>
        <h2>상태별 작업 수</h2>
        {summaryQuery.data && (
          <div className="card-grid">
            {Object.entries(summaryQuery.data.countsByStatus).map(([status, count]) => (
              <div className="card" key={status}>
                <div className="card__label">{status}</div>
                <div className="card__value">{count}</div>
              </div>
            ))}
          </div>
        )}
      </section>

      <section>
        <h2>Gate 지표 (AI 비용 절감 근거)</h2>
        {metricsQuery.data && (
          <div className="card-grid">
            <div className="card">
              <div className="card__label">Gate 1 필터링률</div>
              <div className="card__value">{(metricsQuery.data.gate1FilteredRate * 100).toFixed(1)}%</div>
            </div>
            <div className="card">
              <div className="card__label">Gate 2(LLM) 호출률</div>
              <div className="card__value">{(metricsQuery.data.gate2CallRate * 100).toFixed(1)}%</div>
            </div>
            <div className="card">
              <div className="card__label">평균 confidence</div>
              <div className="card__value">{metricsQuery.data.avgConfidenceScore.toFixed(2)}</div>
            </div>
            <div className="card">
              <div className="card__label">추정 비용 절감</div>
              <div className="card__value">${metricsQuery.data.estimatedCostSavingUsd.toFixed(4)}</div>
            </div>
          </div>
        )}
      </section>
    </div>
  );
}
