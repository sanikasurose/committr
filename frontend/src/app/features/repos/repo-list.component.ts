import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgFor, NgIf } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { of, Subject } from 'rxjs';
import { catchError, finalize, switchMap, tap, timeout } from 'rxjs/operators';
import { RepoService, Repository } from '../../services/repo.service';
import { AddRepoComponent } from './add-repo.component';

@Component({
  selector: 'app-repo-list',
  standalone: true,
  imports: [NgFor, NgIf, AddRepoComponent],
  template: `
    <h1>Repositories</h1>
    <app-add-repo (repoAdded)="onRepoAdded($event)" />

    <p *ngIf="loading">Loading…</p>
    <p *ngIf="refreshing && !loading">Updating…</p>
    <p *ngIf="error && !loading && !refreshing">{{ error }}</p>

    <ul *ngIf="!loading && repos.length">
      <li *ngFor="let repo of repos">
        <a [href]="repo.htmlUrl" target="_blank" rel="noopener noreferrer">{{ repo.fullName }}</a>
        <button type="button" (click)="onDelete(repo.id)" [disabled]="deletingId === repo.id">
          Delete
        </button>
      </li>
    </ul>
    <p *ngIf="!loading && !repos.length && !error && !refreshing">No repositories tracked yet.</p>
  `
})
export class RepoListComponent implements OnInit {
  repos: any[] = [];
  loading = false;
  /** True when re-fetching but we already have rows to show (avoid hiding the list). */
  refreshing = false;
  error: string | null = null;
  deletingId: number | null = null;

  private readonly reloadTrigger = new Subject<void>();
  private loadGeneration = 0;

  private readonly repoService = inject(RepoService);
  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    this.reloadTrigger
      .pipe(
        switchMap(() => this.runLoadRepos()),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();
  }

  ngOnInit(): void {
    this.loadRepos();
  }

  onRepoAdded(repo: Repository): void {
    this.repos = [repo, ...this.repos.filter((r) => r.id !== repo.id)];
    this.loadRepos();
  }

  loadRepos(): void {
    const isInitial = this.repos.length === 0;
    this.loading = isInitial;
    this.refreshing = !isInitial;
    this.error = null;
    this.reloadTrigger.next();
  }

  private runLoadRepos() {
    const gen = ++this.loadGeneration;
    return this.repoService.getRepos().pipe(
      timeout(60_000),
      catchError((err) => {
        this.error = this.extractErrorMessage(err);
        return of([] as Repository[]);
      }),
      tap((repos) => {
        if (gen === this.loadGeneration) {
          this.repos = Array.isArray(repos) ? repos : [];
        }
      }),
      finalize(() => {
        if (gen === this.loadGeneration) {
          this.loading = false;
          this.refreshing = false;
        }
      })
    );
  }

  onDelete(id: number): void {
    this.deletingId = id;
    this.repoService.deleteRepo(id).subscribe({
      next: () => {
        this.deletingId = null;
        this.loadRepos();
      },
      error: (err) => {
        this.deletingId = null;
        this.error = this.extractErrorMessage(err);
      }
    });
  }

  private extractErrorMessage(err: unknown): string {
    if (err instanceof HttpErrorResponse) {
      if (err.status === 0) {
        return 'Cannot reach API (network/CORS). Check that the backend is running on port 8080 and try a hard refresh.';
      }
      if (err.status === 401) {
        return 'Not signed in from this origin — open the app at http://localhost:4200, log in again, then reload /repos.';
      }
      if (typeof err.error === 'string' && err.error.length) {
        return err.error.length < 400 ? err.error : `${err.error.slice(0, 200)}…`;
      }
    }
    if (err && typeof err === 'object' && 'error' in err) {
      const body = (err as { error?: unknown }).error;
      if (body && typeof body === 'object' && 'message' in body) {
        const m = (body as { message?: unknown }).message;
        if (typeof m === 'string') return m;
      }
      if (typeof body === 'string' && body.length) return body;
    }
    if (err && typeof err === 'object' && 'message' in err) {
      const m = (err as { message?: unknown }).message;
      if (typeof m === 'string') return m;
    }
    return 'Request failed.';
  }
}
