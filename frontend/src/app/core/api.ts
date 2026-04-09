import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { apiUrl } from './api-origin';

@Injectable({
  providedIn: 'root'
})
export class ApiService {

  constructor(private http: HttpClient) {}

  /** Uses `apiUrl()` so dev (`ng serve`) hits `:8080` when the API is there. */
  getHealth(): Observable<any> {
    return this.http.get(apiUrl('/api/health'));
  }
}