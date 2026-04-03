import { Component, OnInit } from '@angular/core';
import { NgIf } from '@angular/common';
import { ApiService } from './core/api';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [NgIf], // ✅ REQUIRED
  template: `
    <h1>Committr</h1>
    <p *ngIf="status">Backend status: {{ status }}</p>
  `
})
export class AppComponent implements OnInit {

  status: string = '';

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.api.getHealth().subscribe({
      next: (res) => this.status = res.status,
      error: () => this.status = 'error'
    });
  }
}