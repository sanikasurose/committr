import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-add-repo',
  standalone: true,
  imports: [FormsModule],
  template: `
    <form (ngSubmit)="submit()" class="flex flex-wrap items-center gap-2">
      <div class="relative min-w-0 flex-1 sm:max-w-md">
        <svg width="14" height="14" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true"
             class="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ink-faint">
          <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27s1.36.09 2 .27c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8Z"/>
        </svg>
        <input
          name="fullName"
          type="text"
          placeholder="owner/repo"
          aria-label="Repository (owner/repo)"
          [(ngModel)]="fullNameInput"
          [disabled]="adding"
          autocomplete="off"
          spellcheck="false"
          class="input w-full !pl-9 font-mono"
        />
      </div>
      <button type="submit" [disabled]="adding || !isValid()" class="btn-primary px-4 py-2">
        {{ adding ? 'Adding…' : 'Track repo' }}
      </button>
    </form>
    @if (validationError) {
      <p class="mb-0 mt-2 text-xs text-ink-faint">{{ validationError }}</p>
    }
  `
})
export class AddRepoComponent implements OnChanges {
  @Input() adding = false;
  /** Incremented by parent after a successful add to reset local input. */
  @Input() resetKey = 0;

  @Output() repoAddRequested = new EventEmitter<string>();

  fullNameInput = '';
  validationError: string | null = null;

  isValid(): boolean {
    return this.fullNameInput.trim().includes('/');
  }

  ngOnChanges(changes: SimpleChanges): void {
    const rk = changes['resetKey'];
    if (
      rk &&
      !rk.firstChange &&
      rk.currentValue !== rk.previousValue
    ) {
      this.fullNameInput = '';
      this.validationError = null;
    }
  }

  submit(): void {
    this.validationError = null;
    const name = this.fullNameInput.trim();
    if (!name.includes('/')) {
      this.validationError = 'Invalid format. Use owner/repo';
      return;
    }
    this.repoAddRequested.emit(name);
  }
}
