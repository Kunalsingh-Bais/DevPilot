import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ChatService } from '../../services/chat.service';
import { AskResponse } from '../../models/chat.models';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-chat.component',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.css',
})
export class ChatComponent {

  private readonly chat = inject(ChatService);

  // The form has three fields. nonNullable keeps their values as plain strings, never null.
  readonly form = new FormGroup({
    githubOwner: new FormControl('spring-projects', {
      nonNullable: true,
      validators: [Validators.required],
    }),

    repoName: new FormControl('spring-petclinic', {
      nonNullable: true,
      validators: [Validators.required],
    }),

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
    const { githubOwner, repoName, question } = this.form.getRawValue();

    // Reset the screen for the new question.
    this.loading.set(true);
    this.error.set(null);
    this.response.set(null);

    this.chat.ask({
      githubOwner: githubOwner.trim(),
      repoName: repoName.trim(),
      question: question.trim()
    }).subscribe({
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
        return 'chat-service is not reachable. Start it on port 8083, along with indexing-service and Ollama.';
      }
    }

    return 'Something went wrong. Check the chat-service console for details.';
  }
}
