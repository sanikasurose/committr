import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { apiUrl } from '../core/api-origin';

export interface ContributorShare {
  login: string;
  avatarUrl: string;
  commitCount: number;
  linesAdded: number;
  linesDeleted: number;
  sharePercent: number;
}

export interface LanguageTrend {
  weekStart: string;
  languageDistribution: Record<string, number>;
}

export interface CodingHours {
  hours: number[];
}

export interface CommitFrequency {
  weekStart: string;
  commitCount: number;
  linesAdded: number;
  linesDeleted: number;
}

export interface PrVelocity {
  weekStart: string;
  avgMergeHours: number;
}

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  constructor(private http: HttpClient) {}

  private base(repoId: number): string {
    return apiUrl(`/api/repos/${repoId}/analytics`);
  }

  getContributors(repoId: number): Observable<ContributorShare[]> {
    return this.http.get<ContributorShare[]>(`${this.base(repoId)}/contributors`, { withCredentials: true });
  }

  getLanguageTrend(repoId: number): Observable<LanguageTrend[]> {
    return this.http.get<LanguageTrend[]>(`${this.base(repoId)}/language-trend`, { withCredentials: true });
  }

  getCodingHours(repoId: number): Observable<CodingHours> {
    return this.http.get<CodingHours>(`${this.base(repoId)}/coding-hours`, { withCredentials: true });
  }

  getCommitFrequency(repoId: number): Observable<CommitFrequency[]> {
    return this.http.get<CommitFrequency[]>(`${this.base(repoId)}/commit-frequency`, { withCredentials: true });
  }

  getPrVelocity(repoId: number): Observable<PrVelocity[]> {
    return this.http.get<PrVelocity[]>(`${this.base(repoId)}/pr-velocity`, { withCredentials: true });
  }
}
