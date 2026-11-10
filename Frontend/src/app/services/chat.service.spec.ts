import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { ChatService } from './chat.service';

describe('ChatService', () => {
  let service: ChatService;
  let http: HttpTestingController;

  beforeEach(() => {
    // A fake HTTP backend: requests are recorded, and the test answers them itself.
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ChatService);
    http = TestBed.inject(HttpTestingController);
  });

  // Fails the test if a request was sent but never answered.
  afterEach(() => http.verify());

  it('posts the question to the relative chat address', () => {
    service.ask({ githubOwner: 'spring-projects', repoName: 'spring-petclinic', question: 'Where is the owner?' }).subscribe();

    // expectOne checks that exactly one request went to this address.
    const req = http.expectOne('/api/chat/ask');

    expect(req.request.method).toBe('POST');
    expect(req.request.body.question).toBe('Where is the owner?');

    // Answer the request with a fake response.
    req.flush({ answer: 'ok', commitSha: 'abc', citaions: [], sources: []});
  });
});
