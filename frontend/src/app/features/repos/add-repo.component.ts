import { Component, EventEmitter, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgIf } from '@angular/common';
import { RepoService, Repository } from '../../services/repo.service';

@Component({
  selector: 'app-add-repo',
  standalone: true,
  imports: [FormsModule, NgIf],
  template: `
    <form (ngSubmit)="submit()">
      <label>
        Repository (owner/repo)
        <input
          name="fullName"
          type="text"
          [(ngModel)]="fullNameInput"
          [disabled]="submitting"
          autocomplete="off"
        />
      </label>
      <button type="submit" [disabled]="submitting || !isValid()">Add</button>
    </form>
    <p *ngIf="validationError">{{ validationError }}</p>
    <p *ngIf="error">{{ error }}</p>
  `
})
export class AddRepoComponent {
  @Output() repoAdded = new EventEmitter<Repository>();

  fullNameInput = '';
  submitting = false;
  validationError: string | null = null;
  error: string | null = null;

  constructor(private repoService: RepoService) {}

  isValid(): boolean {
    return this.fullNameInput.trim().includes('/');
  }

  submit(): void {
    this.validationError = null;
    this.error = null;
    const name = this.fullNameInput.trim();
    if (!name.includes('/')) {
      this.validationError = 'Use owner/repo format (must include /).';
      return;
    }
    this.submitting = true;
    this.repoService.addRepo(name).subscribe({
      next: (repo) => {
        this.submitting = false;
        this.fullNameInput = '';
        this.repoAdded.emit(repo);
      },
      error: (err) => {
        this.submitting = false;
        this.error = this.extractErrorMessage(err);
      }
    });
  }

  private extractErrorMessage(err: unknown): string {
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
    return 'Failed to add repository.';
  }
}
