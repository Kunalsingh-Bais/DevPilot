import { Component, inject, input, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ChatService } from '../../services/chat.service';
import { AskResponse } from '../../models/chat.models';
import { HttpErrorResponse } from '@angular/common/http';

// The question panel. It asks about the repository the dashboard selected.
@Component({
  selector: 'app-chat.component',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.css',
})
export class ChatComponent {

  private readonly chat = inject(ChatService);

  // The repository to ask about. The dashboard sets these, so the user never types them here.
  readonly githubOwner = input.required<string>();
  readonly repoName = input.required<string>();

  // The form has one fields. nonNullable keeps their values as plain strings, never null.
  readonly form = new FormGroup({
    question: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(1000)],
    }),
  });

  // Signals: when one of these changes, the template updates automatically.
  readonly loading = signal(false);
  readonly response = signal<AskResponse | null>(null);
  readonly error = signal<string | null>(null);

// ------ Called when the user presses Ask. ------  
  ask(): void {
    // Don't send an invalid form, and don't send a second question while one is running.
    if (this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return; 
    }

    // Read the values and remove extra spaces before sending.
    const question = this.form.controls.question.value.trim();

    // Reset the screen for the new question.
    this.loading.set(true);
    this.error.set(null);
    this.response.set(null);

    this.chat.ask({ githubOwner: this.githubOwner(), repoName: this.repoName(), question }).subscribe({
      next: (result) => {
        this.response.set(result);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(this.readError(err));
        this.loading.set(false);
      },
    });
  }

// ------ Turns any failure into a message the user can act on. ------  
  private readError(err: unknown): string {
    if (err instanceof HttpErrorResponse) {
      // backend returns a ProblemDetail error.
      const detail = err.error?.detail;

      if (typeof detail === 'string') {
        return detail;
      }

      // Status 0 or a proxy error usually means the backend service is not running.
      if (err.status === 0 || err.status === 500 || err.status === 504) {
        return 'chat-service is not reachable. Check that the gateway and the backend services are running.';
      }
    }

    return 'Something went wrong. Check the chat-service console for details.';
  }
}
