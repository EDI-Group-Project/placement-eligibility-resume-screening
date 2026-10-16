# Resume Screening + Staff Tools (backend) - Placement Eligibility & Resume Screening Portal

Pure Java. **No third-party libraries in `src/`** (JDK only, Java 17+). Tests need JUnit 5.

## What this adds on top of the resume module
| # | Feature | Who | Commands |
|---|---------|-----|----------|
| 1 | Filtered student details: on-screen table + Excel (.xlsx) | TPO / TPC / Admin only | `EXPORT_STUDENTS`, `EXPORT_STUDENTS_XLSX` |
| 2 | Batch resume upload (1-20 files): per-file parsing status, match score, eligibility | TPO / TPC / Admin only | `BATCH_START`, `BATCH_ADD_FILE`, `BATCH_STATUS` |
| 3 | Applicants of one job: on-screen table + Excel (.xlsx), **only students who applied** | TPO / TPC / Admin only | `JOB_APPLICANTS`, `JOB_APPLICANTS_XLSX` |
| + | Student applies for a job (creates the rows feature 3 lists; skip if you already have an apply flow) | Student only | `APPLY_JOB` |

Authorization is enforced **on the server** (`RolePolicy`, allow-list). A student, an unknown role, a missing/forged token or a
logged-out session is refused before any parameter is read. The existing endpoints that only checked "has a valid token"
(`GET_STUDENTS`, `SORT_STUDENTS`, `SEARCH_STUDENT`, `GET_ELIGIBLE_STUDENTS`, `SEND_*`, `GET_NOTIFICATIONS`,
`GET_DASHBOARD_STATS`, `ADD_JOB`) are now staff-only too (`integration/ClientHandler.java`). `GET_JOBS` stays public.

## Folder layout (copy each folder into the matching package folder of your project)
```
src/com/placement/
  algorithms/   CandidateBinarySearch, CandidateMergeSort                 (unchanged)
  database/     ApplicationDAO (CHANGED: apply() + update-only score save), ResumeDAO (CHANGED: bulk reads)
  export/       XlsxWriter                                                (NEW - zero-dependency .xlsx writer)
  models/resume AtsFeedbackResult, RankedCandidate, ResumeData, ScreeningResult   (unchanged)
  services/resume/  extractors, parser, screening, ATS, report writer     (unchanged from the verified module)
                    ResumeScreeningService (CHANGED: bulk scoring)
  services/staff/   EligibilityProvider, StudentFilter, StudentRow, ApplicantRow,
                    StudentExportService, ApplicantService, BatchResumeService      (NEW)
  sockets/      RolePolicy, ProtocolIO, StaffCommandHandler, ApplicationCommandHandler   (NEW)
                ResumeCommandHandler (CHANGED: RolePolicy + ProtocolIO)
integration/    ClientHandler.java   <- REPLACES your sockets/ClientHandler.java (changes marked [NEW] / [RBAC])
test/           11 test classes, 107 tests, no MySQL needed (in-memory fakes in testsupport/TestSupport)
sql/            optional_checks.sql  (NO migration needed - read-only checks)
```
Existing project classes this code relies on (not in this zip): `Student`, `JobPosting`, `StudentService`, `JobService`,
`SessionManager`, `DatabaseConnection`, `StudentMergeSort`.

## Setup (5 steps)
1. Copy the folders in. Replace `ClientHandler.java` with `integration/ClientHandler.java` (or apply the marked lines).
2. **Check role names.** Log in once as a TPO and read the server console: `Login attempt [...]: SUCCESS (account type: XXX)`.
   `RolePolicy` expects `TPO`, `TPC`, `ADMIN` (staff) and `STUDENT` (case-insensitive). If your login returns other names, run the server with
   `-Dplacement.staffRoles=TPO,TPC,ADMIN,FACULTY` (or env `PLACEMENT_STAFF_ROLES`) and `-Dplacement.studentRoles=STUDENT`.
   A wrong value fails **closed** ("Access denied"); it never opens access.
3. Resumes are stored in `./resumes/` (see `ResumeScreeningService`); it is git-ignored. Upload limit 2 MB per file.
4. No migration. Run `sql/optional_checks.sql` if you want to confirm the table assumptions.
5. Run the tests (below).

## Protocol (line based, token first; empty line = "no value")
Lists use the existing `SocketClient.sendListRequest` format (`SUCCESS / count / lines / END`); single values use `sendRequest`.

| Command | Lines after the command | Response |
|---|---|---|
| `EXPORT_STUDENTS` | token, jobId, branchesCsv, minCgpa, maxCgpa, eligibility(`ANY`/`ELIGIBLE`/`NOT_ELIGIBLE`), minResumeScore | list: `studentId\|prn\|name\|email\|branch\|cgpa\|backlogs\|eligibility\|resumeScore\|hasResume` |
| `EXPORT_STUDENTS_XLSX` | same | `SUCCESS\|<base64 .xlsx>` |
| `JOB_APPLICANTS` | token, jobId | list: `studentId\|prn\|name\|email\|phone\|branch\|cgpa\|backlogs\|eligibility\|status\|appliedAt\|resumeScore` |
| `JOB_APPLICANTS_XLSX` | token, jobId | `SUCCESS\|<base64 .xlsx>` |
| `BATCH_START` | token, jobId, fileCount (1-20) | `SUCCESS\|<batchId>` |
| `BATCH_ADD_FILE` | token, batchId, fileName, base64Content (one request per file) | `SUCCESS\|<position>` |
| `BATCH_STATUS` | token, batchId | list: `SUMMARY\|expected\|added\|done\|failed\|inProgress\|complete` then `ITEM\|pos\|file\|state\|parseStatus\|prn\|name\|matchScore\|eligibility\|message` |
| `APPLY_JOB` | token, jobId | `SUCCESS\|APPLIED` or `SUCCESS\|ALREADY_APPLIED` |

Eligibility and resume score are **per job**, so those two filters need a `jobId` (otherwise: "Select a job to filter by eligibility or resume score").
Pick the job from the existing public `GET_JOBS`.

### Calling it from the dashboards (existing `SocketClient`)
```java
// table
SocketClient.ListResponse t = client.sendListRequest("EXPORT_STUDENTS", token, jobId, "Computer,IT", "7.5", "", "ELIGIBLE", "40");
// Excel download
SocketClient.Response x = client.sendRequest("JOB_APPLICANTS_XLSX", token, jobId);
if (x.success) Files.write(Path.of("applicants.xlsx"), Base64.getDecoder().decode(x.payload));
// batch: one request per file, then poll
String batchId = client.sendRequest("BATCH_START", token, jobId, String.valueOf(files.size())).payload;
for (Path f : files) client.sendRequest("BATCH_ADD_FILE", token, batchId, f.getFileName().toString(),
        Base64.getEncoder().encodeToString(Files.readAllBytes(f)));
SocketClient.ListResponse s = client.sendListRequest("BATCH_STATUS", token, batchId);   // repeat every ~1 s until SUMMARY|...|true
```
Batch file names must contain the student's **PRN or e-mail**: `B24CE1001.pdf`, `B24CE1001_Tushar.docx`, `aarav@college.edu.pdf`.

## Behaviour you should know about
- **Eligibility** comes from your existing logic (`StudentService.getEligibleUnnotifiedStudents`) - the rules are not duplicated.
  That list excludes students who were already notified, so such a student shows as **"Not eligible / already notified"**.
  For an exact answer add an EligibilityDAO query that ignores notification state and plug it in with one line
  (see `EligibilityProvider`).
- **Batch upload replaces** the student's current resume (the `resumes` table keeps one per student); the item message says so.
  A bad file fails alone; the rest of the batch continues. Batches live in memory for 2 h; processing uses a separate 3-thread pool.
- **Applicants** = rows of `applications` for the job. Screening/ranking never creates an application (`saveScreeningScore` is update-only).
- **Resume score** = the module's 0-100 screening score for that job, computed from the student's current cached resume text.
- PDFs that store text as hex strings (some Google Docs / Canva / "Print to PDF" exports) cannot be read: the item shows `UNREADABLE`.
- `Student.getPhone()` is used for the applicant phone column (the standard getter for the `setPhone` you already call).

## Tests
`test/` holds 11 JUnit 5 classes (107 tests). Maven: add `org.junit.jupiter:junit-jupiter` (test scope) and point `testSourceDirectory` at `test`.
IDEs: mark `test/` as a test root and run all. They need no database: DAOs and services are replaced by in-memory fakes.
Covered: Excel file structure/escaping, role policy, every staff command refusing students/forged tokens/unknown roles,
filters and ordering, applicants-only, a 12-file batch with every kind of failure, 20-file batch, APPLY_JOB, size limits.
