import { Component, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('frontend');

  readonly auth = inject(AuthService);

  constructor() {
    this.auth.loadCurrentUser();
  }

  signOut(): void {
    this.auth.logout();
  }
}
