import { TestBed } from '@angular/core/testing';

import { AuthService } from './auth.service';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('stores the signed-in user when /me succeeds', async () => {
    const pending = service.loadCurrentUser();
    http.expectOne('/api/auth/me').flush({ login: 'octocat', name: 'Octo', avatarUrl: 'https://x' });

    await pending;

    expect(service.user()?.login).toBe('octocat');
  });

  it('clears the user when /me returns 401', async() => {
    const pending = service.loadCurrentUser();
    http.expectOne('/api/auth/me').flush({}, { status: 401, statusText: 'Unauthorized' });

    await pending;

    expect(service.user()).toBeNull();
  })
});
