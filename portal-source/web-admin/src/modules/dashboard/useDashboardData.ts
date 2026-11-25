import { useEffect, useState } from 'react';
import {
  fetchDashboardSnapshotCounts,
  subscribeApplicationsSummary,
  subscribeAuditLogs,
  subscribeCitizenGrievances,
  subscribeRecentNotices,
  subscribeRecentProjects,
} from '../../services/firebase/firestoreService';
import type {
  AuditLogRecord,
  CitizenRequestRecord,
  DashboardMetrics,
  DevelopmentProjectDoc,
  NoticeDoc,
} from '../../types/firestore';

export interface DashboardState {
  loading: boolean;
  metrics: DashboardMetrics;
  recentRequests: CitizenRequestRecord[];
  recentLogs: AuditLogRecord[];
  recentProjects: DevelopmentProjectDoc[];
  recentNotices: NoticeDoc[];
  errors: string[];
}

const INITIAL_METRICS: DashboardMetrics = {
  totalCitizens: null,
  totalProjects: null,
  publishedNotices: null,
  totalApplications: null,
  pendingApplications: null,
  resolvedApplications: null,
  galleryItemsCount: null,
  bloodDonorsCount: null,
  watercraftRoutesCount: null,
  citizenGrievancesCount: null,
  appointmentsCount: null,
};

export function useDashboardData(enabled: boolean): DashboardState {
  const [loading, setLoading] = useState(true);
  const [metrics, setMetrics] = useState<DashboardMetrics>(INITIAL_METRICS);
  const [appRequests, setAppRequests] = useState<CitizenRequestRecord[]>([]);
  const [grievances, setGrievances] = useState<CitizenRequestRecord[]>([]);
  const [recentLogs, setRecentLogs] = useState<AuditLogRecord[]>([]);
  const [recentProjects, setRecentProjects] = useState<DevelopmentProjectDoc[]>([]);
  const [recentNotices, setRecentNotices] = useState<NoticeDoc[]>([]);
  const [errors, setErrors] = useState<string[]>([]);

  useEffect(() => {
    if (!enabled) {
      setLoading(false);
      return;
    }

    let active = true;
    const appendError = (msg: string) => {
      if (!active) return;
      setErrors((prev) => (prev.includes(msg) ? prev : [...prev, msg]));
    };

    fetchDashboardSnapshotCounts()
      .then((counts) => {
        if (!active) return;
        setMetrics((prev) => ({
          ...prev,
          ...counts,
          pendingApplications: prev.pendingApplications,
          resolvedApplications: prev.resolvedApplications,
        }));
      })
      .catch((err) => {
        appendError(err instanceof Error ? err.message : String(err));
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    const unsubApps = subscribeApplicationsSummary(
      (summary) => {
        if (!active) return;
        setMetrics((prev) => ({
          ...prev,
          totalApplications: summary.total,
          pendingApplications: summary.pending,
          resolvedApplications: summary.resolved,
        }));
        setAppRequests(summary.latest);
      },
      appendError
    );

    const unsubGrievances = subscribeCitizenGrievances(
      (items) => {
        if (!active) return;
        setGrievances(items);
      },
      appendError
    );

    const unsubProjects = subscribeRecentProjects(
      (projects, totalCount) => {
        if (!active) return;
        setRecentProjects(projects);
        setMetrics((prev) => ({ ...prev, totalProjects: totalCount }));
      },
      appendError
    );

    const unsubNotices = subscribeRecentNotices(
      (notices, totalCount) => {
        if (!active) return;
        setRecentNotices(notices);
        setMetrics((prev) => ({ ...prev, publishedNotices: totalCount }));
      },
      appendError
    );

    const unsubLogs = subscribeAuditLogs(
      (logs) => {
        if (!active) return;
        setRecentLogs(logs);
      },
      appendError
    );

    return () => {
      active = false;
      unsubApps();
      unsubGrievances();
      unsubProjects();
      unsubNotices();
      unsubLogs();
    };
  }, [enabled]);

  const combinedRequests = [...appRequests, ...grievances].slice(0, 8);

  return {
    loading,
    metrics,
    recentRequests: combinedRequests,
    recentLogs,
    recentProjects,
    recentNotices,
    errors,
  };
}
