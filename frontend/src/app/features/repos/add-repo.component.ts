import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgIf } from '@angular/common';

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
          [disabled]="adding"
          autocomplete="off"
        />
      </label>
      <button type="submit" [disabled]="adding || !isValid()">
        {{ adding ? 'Adding…' : 'Add' }}
      </button>
    </form>
    <p *ngIf="validationError" class="validation-error">{{ validationError }}</p>
    <p *ngIf="addError" class="error" role="alert">{{ addError }}</p>
  `,
  styles: [
    `
      .validation-error,
      .error {
        margin: 0.5rem 0 0;
        font-size: 0.875rem;
      }
      .validation-error {
        color: var(--muted, #64748b);
      }
      .error {
        color: #b91c1c;
      }
    `
  ]
})
export class AddRepoComponent implements OnChanges {
  @Input() adding = false;
  @Input() addError: string | null = null;
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
