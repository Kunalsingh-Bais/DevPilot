import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { AskRequest, AskResponse } from '../models/chat.models';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class ChatService {

  private readonly http = inject(HttpClient);

  // The dev proxy forwards it to chat-service.
  ask(request: AskRequest): Observable<AskResponse> {
    return this.http.post<AskResponse>('/api/chat/ask', request);
  }
}
