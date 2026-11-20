import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { IndexingService } from '../../services/indexing.service';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RepositoryStatus } from '../../models/indexing.models';
import { Subscription } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { ChatComponent } from '../chat.component/chat.component';

// The main page after login. The user indexes a repository here, watches its status, and asks questions once the status is COMPLETED.
@Component({
  selector: 'app-dashboard',
  imports: [ReactiveFormsModule, ChatComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
})
export class DashboardComponent implements OnInit, OnDestroy{

  private readonly indexing = inject(IndexingService);

  // The form for indexing a new repository.
  readonly form = new FormGroup({
    githubOwner: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(100)],
    }),

    repoName: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(100)],
    }),
  });

  readonly repos = signal<RepositoryStatus[]>([]);

  // The repository currently open on the page
  readonly selected = signal<{ githubOwner: string; repoName: string} | null>(null);

  // The latest status of the open repository
  readonly status = signal<RepositoryStatus | null>(null);

  // false until the first status answer arrives
  readonly checked = signal(false);

  readonly busy = signal(false);

  readonly message = signal<string | null>(null);

  // The subscription that keeps checking the status
  private watchSub?: Subscription;

  ngOnInit(): void {
    this.loadRepos();
  }

  ngOnDestroy(): void {
    this.watchSub?.unsubscribe();
  }

  loadRepos(): void {
    this.indexing.list().subscribe({
      next: (list) => this.repos.set(list),
      error: (err: unknown) => this.message.set(readError(err)),
    });
  }

// ------ Called by the Index button in the form ------  
  indexTyped(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const githubOwner = this.form.controls.githubOwner.value.trim();
    const repoName = this.form.controls.repoName.value.trim();
    this.start(githubOwner, repoName);
  }

// ------ Called when the user clicks a repository in the list ------ 
  // It only checks the status, and starts nothing 
  openRepo(githubOwner: string, repoName: string): void {
    this.selected.set({ githubOwner, repoName });
    this.status.set(null);
    this.checked.set(false);
    this.message.set(null);
    this.follow(githubOwner, repoName);
  }

// ------ Starts indexing (or re-indexing) and then follows its progress------  
  start(githubOwner: string, repoName: string): void {
    this.selected.set({ githubOwner, repoName });
    this.status.set(null);
    this.checked.set(false);
    this.message.set(null);
    this.busy.set(true);

    this.indexing.start(githubOwner, repoName).subscribe({
      next: () => {
        this.busy.set(false);
        this.follow(githubOwner, repoName);
        this.loadRepos();
      },
      error: (err: unknown) => {
        this.busy.set(false);
        this.message.set(readError(err));
        // Still show the current status
        this.follow(githubOwner, repoName);
      },
    });
  }

// ------ Watches one repository's status and updates the screen until the job finishes ------  
  private follow(githubOwner: string, repoName: string): void {
    this.watchSub?.unsubscribe();
    this.watchSub = this.indexing.watch(githubOwner, repoName).subscribe({
      next: (s) => {
        this.status.set(s);
        this.checked.set(true);

        // When a job finishes, refresh the list so its badge shows the final state.
        if (s && (s.status === 'COMPLETED' || s.status === 'FAILED')) {
          this.loadRepos();
        }
      },
      error: (err: unknown) => {
        this.checked.set(true);
        this.message.set(readError(err));
      },
    });
  }
}

// ------ Turns any failure into a message the user can act on. ------
function readError(err: unknown): string {
  if (err instanceof HttpErrorResponse) {
    const detail = err.error?.detail;

    if (typeof detail === 'string') {
      return detail;
    }

    if (err.status === 0) {
      return 'The backend is not reachable. Check that the gateway and the services are running.';
    }
  }
  return 'Something went wrong. Check the service console for details.';
}
