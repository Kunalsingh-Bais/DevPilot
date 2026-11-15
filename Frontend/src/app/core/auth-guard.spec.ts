import { TestBed } from '@angular/core/testing';
import { CanActivateFn, provideRouter, UrlTree } from '@angular/router';

import { authGuard } from './auth-guard';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

describe('authGuard', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });

  // Fails the test if a request was sent but never answered
  afterEach(() => http.verify());

  // Runs the guard inside Angular's dependency injection
  function runGuard(): Promise<unknown> {
    return TestBed.runInInjectionContext(() => authGuard(null!, null!)) as Promise<unknown>;
  }

  it('allows the page when someone is signed in', async () => {
    const pending = runGuard();
    http.expectOne('/api/auth/me').flush({ login: 'octocat', name: null, avatarUrl: '' });

    expect(await pending).toBe(true);
  });

  it('redirects to /login when nobody is signed in', async () => {
    const pending = runGuard();
    http.expectOne('/api/auth/me').flush({}, { status: 401, statusText: 'Unauthorized' });

    const result = (await pending) as UrlTree;
    expect(result.toString()).toBe('/login');
  });
});
