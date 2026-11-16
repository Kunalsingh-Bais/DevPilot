import { Component, inject } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { ActivatedRoute } from '@angular/router';

// The sign-in page. It has one job: start the GitHub login.
@Component({
  selector: 'app-login.component',
  imports: [],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent {

  // template can call auth.login().
  readonly auth = inject(AuthService);

  // True when the backend sent the user back here after a failed GitHub login
  readonly failed = inject(ActivatedRoute).snapshot.queryParamMap.has('error');
}
