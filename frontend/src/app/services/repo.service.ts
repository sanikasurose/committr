import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { apiUrl } from '../core/api-origin';

export interface Repository {
  id: number;
  githubRepoId: number;
  name: string;
  fullName: string;
  ownerLogin: string;
  isPrivate: boolean;
  htmlUrl: string;
  createdAt: string;
  updatedAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class RepoService {
  constructor(private http: HttpClient) {}

  private reposBase(): string {
    return apiUrl('/api/repos');
  }

  getRepos(): Observable<Repository[]> {
    return this.http.get<Repository[]>(this.reposBase(), { withCredentials: true });
  }

  addRepo(fullName: string): Observable<Repository> {
    return this.http.post<Repository>(this.reposBase(), { fullName }, { withCredentials: true });
  }

  deleteRepo(id: number): Observable<void> {
    return this.http.delete<void>(`${this.reposBase()}/${id}`, { withCredentials: true });
  }
}
