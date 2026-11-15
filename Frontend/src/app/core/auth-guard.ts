import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

// Opens a page only when someone is signed in. Otherwise it sends the user to login
export const authGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  await auth.loadCurrentUser();

  // Returning a UrlTree redirects the user to that address instead of opening the page
  return auth.user() ? true : router.createUrlTree(['/login']);
};

// Opens the login page only for people who are NOT signed in.
export const guestGaurd: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  await auth.loadCurrentUser();

  return auth.user() ? router.createUrlTree(['/dashboard']) : true;
}
