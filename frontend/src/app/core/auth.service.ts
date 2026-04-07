import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  currentUser$ = new BehaviorSubject<any | null>(null);

  constructor(private http: HttpClient) {}

  login(): void {
    window.location.href = 'http://localhost:8080/api/auth/login';
  }

  getCurrentUser(): Observable<any> {
    return this.http.get('/api/auth/me', { withCredentials: true });
  }

  logout(): Observable<unknown> {
    return this.http.post('/api/auth/logout', {}, { withCredentials: true });
  }
}
