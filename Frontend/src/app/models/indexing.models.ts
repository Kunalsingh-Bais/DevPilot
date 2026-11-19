// The status an indexing job can be in. They match the backend enum.
export type IndexingState = 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';

// The JSON that indexing-service returns for one repository the user has indexed
export interface RepositoryStatus {
    id: number;
    githubOwner: string;
    repoName: string;
    status: IndexingState;
    commitSha: string | null;
    errorMessage: string | null;
    createdAt: string;
}