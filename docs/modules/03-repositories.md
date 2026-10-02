# 03 · Repositories

**Packages:** `com.jobportal.repository` (interfaces), `com.jobportal.repository.inmemory` (implementations)

Repositories hide *where* data is stored. Services depend only on the interfaces, so the storage can change (in-memory today, JDBC/PostgreSQL in Phase 2) without touching business logic.

## Generic contract

```java
public interface Repository<T, ID> {
    T save(T entity);            // insert (assigns id) or update
    Optional<T> findById(ID id);
    List<T> findAll();
    boolean existsById(ID id);
    void deleteById(ID id);
    long count();
}
```

## Repositories and their extra queries

| Interface | Extra methods |
|-----------|---------------|
| `UserRepository` | `findByEmail(email)` (case-insensitive), `findByRole(role)` |
| `CompanyRepository` | none |
| `JobRepository` | `findByCompanyId(companyId)` |
| `ApplicationRepository` | `findByJobId`, `findByCandidateId`, `findByJobIdAndCandidateId` |
| `InterviewRepository` | `findByInterviewerEmail` (case-insensitive), `findByCandidateId` |
| `NotificationRepository` | `findByRecipientUserId` |

## In-memory implementation

`InMemoryRepository<T extends Identifiable>` is the shared base class:

- storage: `ConcurrentHashMap<Long, T>`, because notification worker threads and concurrent HTTP requests access it at the same time
- ids: `AtomicLong` sequence, assigned on first `save` when `getId() == null`
- `findAll()` returns a **copy** (`new ArrayList<>(values)`), so callers can't corrupt the store
- `findWhere(Predicate<T>)` helper used by subclasses for their custom queries

Each concrete class (`InMemoryJobRepository`, …) extends the base and implements its interface's extra methods with a stream filter.

### Things to know

- Objects are stored **by reference**. Changing a returned object changes the stored one even without `save()`. Services still call `save()` after every change so they keep working once repositories copy data (JDBC).
- Data lives only as long as the JVM: restarting the backend wipes everything (`DemoData` reloads the samples).
- Queries are O(n) scans. That's fine for a demo; Phase 2 gets indexes.

## Phase 2: adding a JDBC implementation

1. Design the tables (`users`, `companies`, `jobs`, `job_skills`, `candidate_skills`, `applications`, `interviews`, `interview_feedback`, `notifications`, …).
2. Create e.g. `repository/jdbc/JdbcJobRepository implements JobRepository` using `PreparedStatement` (never string-concatenate SQL).
3. Swap the `new InMemory…Repository()` lines in `PortalApplication` for the JDBC ones. Nothing else changes.
4. Add `UNIQUE(job_id, candidate_id)` on `applications`, which closes the race condition described in TODO #8.
5. Run the existing service tests: they define the behaviour the new implementation must keep.
