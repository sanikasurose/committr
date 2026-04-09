import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { NgFor, NgIf } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { RepoService, Repository } from '../../services/repo.service';
import { AddRepoComponent } from './add-repo.component';

@Component({
  selector: 'app-repo-list',
  standalone: true,
  imports: [NgFor, NgIf, AddRepoComponent],
  template: `
    <h1>Repositories</h1>
    <app-add-repo
      [adding]="adding"
      [addError]="addError"
      [resetKey]="addSuccessKey"
      (repoAddRequested)="onAddRequested($event)"
    />

    <p *ngIf="loading && repos.length === 0" class="status">Loading repositories…</p>
    <p *ngIf="loading && repos.length > 0" class="status muted">Updating list…</p>

    <div *ngIf="error" class="error-banner" role="alert">{{ error }}</div>

    <ul *ngIf="!loading || repos.length > 0" class="repo-list">
      <li *ngFor="let repo of repos">
        <a [href]="repo.htmlUrl" target="_blank" rel="noopener noreferrer">{{ repo.fullName }}</a>
        <button
          type="button"
          (click)="onDelete(repo.id)"
          [disabled]="deletingId === repo.id"
          [attr.aria-busy]="deletingId === repo.id"
        >
          {{ deletingId === repo.id ? 'Deleting…' : 'Delete' }}
        </button>
      </li>
    </ul>

    <p *ngIf="!loading && repos.length === 0 && !error" class="empty">No repositories added yet</p>
  `,
  styles: [
    `
      .status {
        margin: 0.75rem 0;
      }
      .status.muted {
        font-size: 0.875rem;
        color: var(--muted, #64748b);
      }
      .error-banner {
        margin: 0.75rem 0;
        padding: 0.5rem 0.75rem;
        border-radius: 6px;
        background: color-mix(in srgb, #ef4444 12%, transparent);
        color: #b91c1c;
      }
      .repo-list {
        list-style: none;
        padding: 0;
        margin: 1rem 0 0;
      }
      .repo-list li {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        margin-bottom: 0.5rem;
      }
      .empty {
        margin-top: 1rem;
        color: var(--muted, #64748b);
      }
    `
  ]
})
export class RepoListComponent implements OnInit {
  repos: Repository[] = [];
  loading = false;
  error: string | null = null;
  adding = false;
  addError: string | null = null;
  /** Increment after a successful add so the child form can reset. */
  addSuccessKey = 0;
  deletingId: number | null = null;

  private loadId = 0;

  private readonly repoService = inject(RepoService);
  private readonly cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.loadRepos();
  }

  loadRepos(): void {
    this.loading = true;
    this.error = null;
    this.cdr.markForCheck();
    const id = ++this.loadId;
    this.repoService.getRepos().subscribe({
      next: (data) => {
        if (id !== this.loadId) return;
        this.repos = Array.isArray(data) ? data : [];
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        if (id !== this.loadId) return;
        this.error = this.extractErrorMessage(err) || 'Failed to load repositories';
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  onAddRequested(fullName: string): void {
    this.adding = true;
    this.addError = null;
    this.cdr.markForCheck();
    this.repoService.addRepo(fullName).subscribe({
      next: () => {
        this.adding = false;
        this.addError = null;
        this.addSuccessKey++;
        this.cdr.markForCheck();
        this.loadRepos();
      },
      error: (err) => {
        this.adding = false;
        this.addError = this.extractErrorMessage(err) || 'Failed to add repository';
        this.cdr.markForCheck();
      }
    });
  }

  onDelete(id: number): void {
    this.deletingId = id;
    this.error = null;
    this.cdr.markForCheck();
    this.repoService.deleteRepo(id).subscribe({
      next: () => {
        this.deletingId = null;
        this.cdr.markForCheck();
        this.loadRepos();
      },
      error: (err) => {
        this.deletingId = null;
        this.error = this.extractErrorMessage(err) || 'Failed to delete repository';
        this.cdr.markForCheck();
      }
    });
  }

  private extractErrorMessage(err: unknown): string | null {
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
    return null;
  }
}
