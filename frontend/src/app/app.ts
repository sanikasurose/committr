import { Component, HostListener, OnInit, signal } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs/operators';
import { ApiService } from './core/api';
import { AuthService } from './core/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [AsyncPipe, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    @if (!isLoginPage()) {
      <header class="sticky top-0 z-40 border-b border-edge bg-bg/85 backdrop-blur">
        <div class="mx-auto flex h-14 max-w-6xl items-center gap-6 px-4 sm:px-6">
          <a routerLink="/" class="flex items-center gap-2.5 text-ink no-underline">
            <span class="flex h-6 w-6 items-center justify-center rounded-md bg-accent-strong">
              <!-- git commit glyph -->
              <svg width="14" height="14" viewBox="0 0 16 16" fill="none" aria-hidden="true">
                <circle cx="8" cy="8" r="3" stroke="white" stroke-width="1.8"/>
                <path d="M8 0v3.5M8 12.5V16" stroke="white" stroke-width="1.8"/>
              </svg>
            </span>
            <span class="text-[15px] font-semibold tracking-tight">Committr</span>
          </a>

          @if (authService.currentUser$ | async; as user) {
            <nav class="flex items-center gap-1 text-sm">
              <a routerLink="/repos" routerLinkActive="!text-ink !bg-surface-2"
                 class="rounded-md px-3 py-1.5 text-ink-mute no-underline transition-colors hover:text-ink">
                Repositories
              </a>
              <a [routerLink]="['/u', user.username]" routerLinkActive="!text-ink !bg-surface-2"
                 class="rounded-md px-3 py-1.5 text-ink-mute no-underline transition-colors hover:text-ink">
                Profile &amp; badge
              </a>
            </nav>
          }

          <div class="ml-auto flex items-center gap-4">
            <span class="hidden items-center gap-1.5 font-mono text-[11px] text-ink-faint sm:flex"
                  [title]="'Backend status: ' + (apiStatus() || 'checking')">
              <span class="h-1.5 w-1.5 rounded-full"
                    [class]="apiStatus() === 'ok' ? 'bg-ok' : apiStatus() === '' ? 'bg-ink-faint' : 'bg-danger'"></span>
              API
            </span>

            @if (authService.currentUser$ | async; as user) {
              <div class="relative">
                <button type="button" (click)="toggleMenu($event)"
                        class="flex cursor-pointer items-center gap-2 rounded-full border border-edge bg-surface py-1 pl-1 pr-2.5 transition-colors hover:border-edge-strong">
                  <img [src]="user.avatarUrl" alt="" width="24" height="24" class="rounded-full" />
                  <span class="text-sm text-ink">{{ user.username }}</span>
                  <svg width="10" height="10" viewBox="0 0 10 10" fill="none" class="text-ink-faint" aria-hidden="true">
                    <path d="M2 3.5L5 6.5L8 3.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                  </svg>
                </button>

                @if (menuOpen()) {
                  <div class="absolute right-0 top-full mt-2 w-56 overflow-hidden rounded-lg border border-edge bg-surface-2 py-1 shadow-2xl shadow-black/50">
                    <p class="m-0 px-3 py-2 text-xs text-ink-faint">
                      Signed in as <span class="font-mono text-ink-mute">{{ user.username }}</span>
                    </p>
                    <div class="my-1 h-px bg-edge"></div>
                    <a [routerLink]="['/u', user.username]" (click)="menuOpen.set(false)"
                       class="block px-3 py-2 text-sm text-ink-mute no-underline transition-colors hover:bg-surface-3 hover:text-ink">
                      Public profile
                    </a>
                    <a [routerLink]="['/u', user.username]" fragment="embed" (click)="menuOpen.set(false)"
                       class="block px-3 py-2 text-sm text-ink-mute no-underline transition-colors hover:bg-surface-3 hover:text-ink">
                      Badge embed snippet
                    </a>
                    <div class="my-1 h-px bg-edge"></div>
                    <button type="button" (click)="logout()"
                            class="block w-full cursor-pointer px-3 py-2 text-left text-sm text-danger transition-colors hover:bg-surface-3">
                      Log out
                    </button>
                  </div>
                }
              </div>
            } @else {
              <a routerLink="/login" class="btn-secondary px-3 py-1.5">Sign in</a>
            }
          </div>
        </div>
      </header>
    }

    <router-outlet />
  `
})
export class AppComponent implements OnInit {
  readonly apiStatus = signal('');
  readonly menuOpen = signal(false);
  readonly isLoginPage = signal(false);

  constructor(
    private api: ApiService,
    private router: Router,
    public authService: AuthService
  ) {
    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe((e) => {
        this.isLoginPage.set(e.urlAfterRedirects.startsWith('/login'));
        this.menuOpen.set(false);
      });
  }

  ngOnInit(): void {
    this.authService.getCurrentUser().subscribe({
      next: (user) => this.authService.currentUser$.next(user),
      error: () => this.authService.currentUser$.next(null)
    });

    this.api.getHealth().subscribe({
      next: (res) => this.apiStatus.set(res.status),
      error: () => this.apiStatus.set('error')
    });
  }

  toggleMenu(event: Event): void {
    event.stopPropagation();
    this.menuOpen.update((open) => !open);
  }

  @HostListener('document:click')
  closeMenu(): void {
    if (this.menuOpen()) this.menuOpen.set(false);
  }

  logout(): void {
    this.authService.logout().subscribe(() => {
      this.authService.currentUser$.next(null);
      this.router.navigate(['/login']);
    });
  }
}
