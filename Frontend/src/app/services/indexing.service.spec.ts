import { TestBed } from '@angular/core/testing';

import { IndexingService } from './indexing.service';

describe('IndexingService', () => {
  let service: IndexingService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(IndexingService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
