import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, of, switchMap, takeWhile, throwError, timer } from 'rxjs';
import { RepositoryStatus } from '../models/indexing.models';

const POLL_MILLISECONDS = 3000;

@Injectable({
  providedIn: 'root',
})
export class IndexingService {

  private readonly http = inject(HttpClient);

// --- Every repository the signed-in user has indexed. ---
  list(): Observable<RepositoryStatus[]> {
    return this.http.get<RepositoryStatus[]>('/api/indexing/repositories');
  }

// --- Starts indexing for the signed-in user --- 
  start(githubOwner: string, repoName: string): Observable<RepositoryStatus> {
    return this.http.post<RepositoryStatus>('/api/indexing/repositories', {githubOwner, repoName});
  }

// --- Current status of one repository ---
  status(githubOwner: string, repoName: string): Observable<RepositoryStatus | null> {
    const url = `/api/indexing/repositories/${encodeURIComponent(githubOwner)}/${encodeURIComponent(repoName)}`;

    return this.http.get<RepositoryStatus>(url).pipe(
      catchError((err: HttpErrorResponse) => err.status === 404 ? of(null) : throwError(() => err))
    );
  }

// --- Checks the status every few seconds while the job is PENDING or RUNNING ---  
  watch(githubOwner: string, repoName: string): Observable<RepositoryStatus | null> {
    return timer(0, POLL_MILLISECONDS).pipe(
      switchMap(() => this.status(githubOwner, repoName)),
      takeWhile(s => s !== null && (s.status === 'PENDING' || s.status === 'RUNNING'), true)
    );
  }

}
