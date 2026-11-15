import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

// The signed-in user, as returned by GET /api/auth/me
export interface CurrentUser {
  login: string;
  name: string | null;
  avatarUrl: string;
}

@Injectable({
  providedIn: 'root',
})

// Holds who is signed in.
export class AuthService {

  private readonly http = inject(HttpClient);

  // null means "not signed in"
  readonly user = signal<CurrentUser | null>(null);

// ------ Ask the server who is signed in ------  
  async loadCurrentUser(): Promise<void> {
    try {
      const me = await firstValueFrom(this.http.get<CurrentUser>('/api/auth/me'));

      this.user.set(me);
    }
    catch (err) {
      // 401 is "not signed in"
      this.user.set(null);
    }
  }

//--- A full page navigation, because GitHub's sign-in page must open in the browser, not through HttpClient. ---
  login(): void {
    window.location.href = '/api/auth/login/github';
  }

// ------ Ends the server session, then clears the signed-in state in the  browser ------  
  async logout(): Promise<void> {
    try {
      await firstValueFrom(this.http.post<void>('/api/auth/login', null));
    }
    catch (err) {
      // if the server call fails, the screen should show signed out.
    }
    finally {
      this.user.set(null);
    }
  }
}
