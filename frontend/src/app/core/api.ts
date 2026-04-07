import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ApiService {

  constructor(private http: HttpClient) {}

  /** Same-origin `/api` in dev (via `proxy.conf.json`) avoids browser CORS vs `localhost:8080`. */
  getHealth(): Observable<any> {
    return this.http.get('/api/health');
  }
}