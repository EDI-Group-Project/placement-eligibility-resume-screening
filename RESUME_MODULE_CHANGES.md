# Resume Screening Module: new features, API and test results

Backend only. The Swing frontend is not touched and the database schema is not changed.

## 1. What was added

| # | Feature | Commands | Allowed roles |
|---|---------|----------|---------------|
| 1 | Export filtered students | `EXPORT_STUDENTS` (Excel). Table view: the existing `SEARCH_SORT_FILTER_STUDENTS` | ADMIN, TPO, TPC |
| 2 | Batch resume upload, 1 to 20 resumes | `UPLOAD_RESUME_BATCH`, `GET_RESUME_BATCH` | ADMIN, TPO, TPC |
| 3 | Job applicant list | `GET_JOB_APPLICANTS` (table), `EXPORT_JOB_APPLICANTS` (Excel) | ADMIN, TPO, TPC |

STUDENT, DIRECTOR, DEAN and any other role get `FAILED|You do not have permission for this operation.` on all of them. A missing or expired token gets `FAILED|Not authenticated. Please log in again.`

All new commands live in `ResumeCommandHandler`, which `ClientHandler` already delegates to. They reuse the existing session tokens, `StudentService.query` (the same filter code as the table), `JobService`, `ApplicationDAO`, and the single-resume pipeline (`ResumeScreeningService.uploadResume`, ATS feedback and job screening). No new tables and no new libraries: the Excel files are written by a small zero-dependency writer, the same approach as the existing `MinimalPdfWriter`.

## 2. Files

Changed (4):

- `sockets/ResumeCommandHandler.java`: the five new commands, staff check, bounded request reading, Excel/list replies.
- `sockets/ClientHandler.java`: one word. `SEARCH_SORT_FILTER_STUDENTS` now also allows TPO (it allowed only ADMIN and TPC, so a TPO could not view the table).
- `database/ApplicationDAO.java`: new `findApplicantsByJob`, and `saveScreeningScore` changed (see section 3).
- `services/resume/ResumeScreeningService.java`: new `hasResume()` (uses the existing `ResumeDAO.existsForStudent`) and a corrected Javadoc.

New (3):

- `services/resume/ExportService.java`: column layout of the two Excel files, in one place.
- `services/resume/XlsxWriter.java`: one-sheet .xlsx writer. Bold header, frozen header row, sized columns, text is never written as a formula (so `=cmd|...` in a student's skills stays plain text in Excel), characters that make Excel report a corrupt file are removed.
- `services/resume/ResumeBatchService.java`: batch processing, per-file status, recent-batch lookup.

New tests: `XlsxWriterTest`, `ExportServiceTest`, `ResumeBatchServiceTest`, `ResumeFeatureCommandsTest`, `ResumeFeaturesIntegrationTest`, and the helpers `testsupport/Fakes` and `testsupport/XlsxReader`. Existing test files are untouched.

## 3. One behaviour change in existing code

`ApplicationDAO.saveScreeningScore` used to INSERT a `SCREENED` application row when the student had none. So screening or ranking a student who never applied made them look like an applicant, and their own `APPLY_JOB` then failed with "already applied". Feature 3 needs a row in `applications` to mean "the student applied", so the method now only UPDATEs an existing application. Screening results are the same as before. Only the side effect is gone.

Rows created by the old behaviour before this fix look exactly like real applications (same columns), so they cannot be told apart and will still appear in an applicant list. If your data has such rows, review applications with status `SCREENED` that the student did not make.

## 4. API reference

Protocol rules are the same as the rest of the server: send the command name, then one value per line, the session token first. Replies:

- Error: one line, `FAILED|<message>`. Show the message to the user.
- List: `SUCCESS`, then a count, then that many lines, then `END` (read with `SocketClient.sendListRequest`).
- File: one line, `SUCCESS|<base64 UTF-8 file name>|<base64 file bytes>`. Same shape as `DOWNLOAD_RESUME` (read with `SocketClient.sendRequest`, then `parts()`).

### 4.1 Export filtered students

Table view, already existing: `SEARCH_SORT_FILTER_STUDENTS`, list reply, 10 fields per row:
`prn|name|email|dept|cgpa|year|backlogs|semester|phone|skills`. Now open to ADMIN, TPO and TPC.

Excel export: `EXPORT_STUDENTS`, file reply. It takes the same 8 filter lines, in the same order, as the table command, so the export button can send whatever the filters currently show. The file always equals the table.

```
EXPORT_STUDENTS
<token>
<search>        matches PRN, name, email or department (contains, ignore case). Empty = all
<dept>          department. Empty or ALL = all
<year>          passing year. 0 = all
<minCgpa>       minimum CGPA. -1 or empty = no minimum
<maxBacklogs>   maximum backlogs. 0 = all (see 6.4)
<semester>      1 to 8. 0 = all
<sortField>     CGPA (default), NAME, DEPARTMENT, YEAR, BACKLOGS, SEMESTER or PRN
<ascending>     true or false
```

File name: `students_<yyyy-MM-dd>.xlsx`. Sheet `Students`. Columns: PRN, Name, Email, Department, CGPA, Passing Year, Backlogs, Semester, Phone, Skills. No matches gives a file with the header row only. The password hash is never exported.

### 4.2 Job applicants

```
GET_JOB_APPLICANTS            (list reply)         EXPORT_JOB_APPLICANTS        (file reply)
<token>                                            <token>
<jobId>   the job id from GET_JOBS                 <jobId>
```

Only students who have an application for that job are returned, oldest first. The list has 13 fields per row. The first 10 are exactly the student line above, then:

```
...|applicationStatus|screeningScore|appliedAt
```

- `applicationStatus`: `APPLIED`, or `SCREENED` once the application has been screened.
- `screeningScore`: empty until the application has been screened.
- `appliedAt`: `yyyy-MM-dd HH:mm:ss`.

Example:

```
SUCCESS
2
PRN001|Aarav Sharma|aarav@gmail.com|CSE|8.5|2027|0|5|9876543210|Java,SQL,Python|APPLIED||2026-10-15 18:45:11
PRN002|Priya Patil|priya@gmail.com|IT|7.8|2027|0|5|9876543211|Java,React,SQL|APPLIED||2026-10-15 18:45:11
END
```

File name: `applicants_<jobId>_<yyyy-MM-dd>.xlsx`. Sheet `Applicants`. Columns: the 10 student columns, then Application Status, Screening Score, Applied At. A job with no applicants gives an empty list, or a file with the header row only.

Errors: `FAILED|Job ID is required.`, `FAILED|Job not found: <jobId>`.

### 4.3 Batch resume upload

```
UPLOAD_RESUME_BATCH
<token>
<jobId>         optional. Empty line = no screening. With a job, each readable resume also gets a screening score
<count>         1 to 20
then, count times:
<studentRef>    student id, PRN or email (the resumes table keeps one resume per student)
<fileName>      .pdf, .docx or .txt
<base64>        the file bytes on ONE line (use Base64.getEncoder(), not the MIME encoder)
```

Each file is checked and processed on its own with the same code as a single upload (type, 2 MB size limit, content check, text extraction, stored file, ATS feedback). One bad file never stops the others. A student who already has a resume gets it replaced (the row says so). Two files for the same student in one batch: the first is kept, the second fails.

Reply (list): one `BATCH` line, then one `RESULT` line per file, in request order.

```
BATCH|batchId|total|processed|unreadable|failed|jobId
RESULT|index|studentRef|studentId|prn|name|fileName|status|words|atsScore|atsWarnings|skills|screeningTotal|skillScore|matchedSkills|missingSkills|message
```

`RESULT` has 17 fields:

1. `RESULT`
2. `index`, 1-based, same order as the request
3. `studentRef`, exactly what was sent
4. `studentId`, empty if the student was not found
5. `prn`
6. `name`
7. `fileName`
8. `status`: `PROCESSED` (stored and text read), `UNREADABLE` (stored, but no text could be read) or `FAILED` (nothing stored)
9. `words`, word count of the resume text
10. `atsScore`
11. `atsWarnings`, joined with `; `
12. `skills`, skills found in the resume, comma separated
13. `screeningTotal`, empty unless a job was given and the resume is readable
14. `skillScore`
15. `matchedSkills`, comma separated
16. `missingSkills`, comma separated
17. `message`, the reason for FAILED or a note such as "Replaced the previous resume."

Example (job `JOB001`, four files):

```
SUCCESS
5
BATCH|b0199c91-7cf9-4506-ac87-5e870809a73e|4|2|0|2|JOB001
RESULT|1|PRN001|1|PRN001|Aarav Sharma|aarav_resume.txt|PROCESSED|26|16|No "Skills" section was detected.; ...|java,mysql,python,sql|41.0|40.0|java,sql||
RESULT|2|priya@gmail.com|2|PRN002|Priya Patil|priya_resume.txt|PROCESSED|20|16|No "Skills" section was detected.; ...|java,oracle,react,sql|40.0|40.0|java,sql||
RESULT|3|PRN999||||nobody.txt|FAILED|||||||||Student not found: PRN999
RESULT|4|PRN003|3|PRN003|Rohan Mehta|rohan.exe|FAILED|||||||||Only PDF, DOCX or TXT resumes are accepted.
END
```

Whole-request errors (nothing is processed): `FAILED|A batch must contain 1 to 20 resumes.`, `FAILED|Job not found: <jobId>`, permission and token errors above.

Batch screening is read-only. It shows the same score `SCREEN_STUDENT` would, but never creates or changes an application.

To open the stored file of a row, send its `studentId` to the existing `DOWNLOAD_RESUME` (staff can already use it).

```
GET_RESUME_BATCH
<token>
<batchId>       from the BATCH line
```

Returns the same list again. Only the 50 most recent batches are kept, in server memory, so after a restart it answers `FAILED|Batch not found. Only recent batches are kept, and they are cleared when the server restarts.`

## 5. What the frontend team has to build

Nothing was changed in the frontend. These are the needed changes:

1. Student table screen (TPO, TPC, Admin dashboards): add an "Export to Excel" button that sends `EXPORT_STUDENTS` with the filter values currently applied, then saves the decoded bytes through a file chooser. Make sure the TPO dashboard can open the existing student search/filter screen (`SEARCH_SORT_FILTER_STUDENTS` now accepts TPO).
2. Job applicants screen (TPO, TPC, Admin): a job picker (from `GET_JOBS`), a 13-column table from `GET_JOB_APPLICANTS`, and a "Download Excel" button using `EXPORT_JOB_APPLICANTS`.
3. Batch upload screen (TPO, TPC, Admin): multi-select 1 to 20 files (pdf, docx, txt, up to 2 MB each), a student reference per file (id, PRN or email), an optional job, then a results table from the `RESULT` lines with a summary from the `BATCH` line. Offer `DOWNLOAD_RESUME` per row if wanted, and `GET_RESUME_BATCH` to reopen a recent batch.
4. Timeouts: `SocketClient` has a 10 second read timeout and no way to change it. The batch answer comes after all files are processed, so add a longer read timeout for `UPLOAD_RESUME_BATCH` (120 to 180 seconds) and show a waiting state. Measured: 20 DOCX files take under 1 second, 20 text-heavy PDFs about 15 seconds, 20 PDFs of the 2 MB maximum about 70 seconds.
5. Show `FAILED|` messages as they are. For `UNREADABLE` rows, tell the user to upload a .docx or a text-based PDF.
6. Hide these screens from STUDENT, DIRECTOR and DEAN. The server enforces it anyway.

Decoding a file reply (same as `DOWNLOAD_RESUME`):

```java
SocketClient.Response r = client.sendRequest("EXPORT_STUDENTS", token, search, dept, year, minCgpa, maxBacklogs, semester, sortField, ascending);
if (!r.success) { /* r.payload is the message */ }
String[] p = r.parts();   // [base64 file name, base64 bytes]
String fileName = new String(Base64.getDecoder().decode(p[0]), StandardCharsets.UTF_8);
byte[] xlsx = Base64.getDecoder().decode(p[1]);
```

## 6. Known limits and decisions

1. Batch results are kept in server memory only (last 50 batches, lost on restart). There is no batch table in the schema and none was invented.
2. No transaction spans a batch. Each file is stored on its own, and the row status says exactly what happened.
3. The existing PDF text extractor cannot read some PDFs (for example some Word or LibreOffice exports that use embedded font encodings). Single upload returns `UNREADABLE` for them too. DOCX and TXT are reliable.
4. `year`, `maxBacklogs` and `semester` use 0 for "all", as the existing table command does (the frontend sends 0 for ALL). So "exactly 0 backlogs" cannot be filtered. The export deliberately behaves the same so the file equals the table.
5. The SRS role matrix lists only Student and Admin for resume upload. Batch upload for TPO and TPC follows the request for this feature.
6. Times (`appliedAt`, the date in file names) use the JVM time zone, the same as the existing "my applications" screen. The JDBC URL in `application.properties` says `serverTimezone=UTC`; if your MySQL server is not in UTC, times can be shifted. This is unchanged from today.
7. Stored resume files are named `student_<id>_<millis>.<ext>` (existing). Two uploads for the same student in the same millisecond would collide. A batch never does that, because it keeps one file per student.
8. Each resume in a batch is parsed once on upload and read back from the database for the analysis, so a batch costs about twice the work of a single upload.
9. Applications created by the old screening behaviour cannot be identified (section 3).

## 7. Tests

Run:

```
mvn test                              # no database needed, database tests are skipped
mvn -DRUN_DB_TESTS=true test          # needs MySQL set up from sql/ and DB_URL, DB_USER, DB_PASSWORD
```

Results of the final run: 112 tests, 112 passed with MySQL (50 existing, 62 new). Without the database flag: 99 passed, 13 skipped because they need MySQL (4 existing, 9 new), 0 failed.

What the new tests cover:

- Excel writer and export: the file is a valid package, read back with a separate JDK-only reader. XML escaping, formula-looking text, illegal characters, long cells, sheet names, number cells, columns, no password column.
- Batch: files processed independently, every status, lookup by id, PRN and email, duplicates, oversize and bad Base64 files, replace note, analysis failure not counted as FAILED, field escaping, recent-batch lookup.
- Commands: each command refused for STUDENT, DIRECTOR, DEAN, other roles, forged and empty tokens, and allowed for ADMIN, TPO and TPC. Validation messages, filter arguments identical to the table command, truncated requests, over-long lines, internal errors not leaked to the client, a refused big upload still delivering its reason.
- End to end through the real `ClientHandler` socket server and MySQL: exports equal the table, applicants are only real applicants, screening does not create applicants (this test fails on the old code), a 20-resume batch, stored files and rows removed afterwards. These tests skip instead of touching existing rows.

Also checked by hand: the .xlsx files open in LibreOffice and openpyxl.

The test run used `javac` and the JUnit console launcher with MySQL 8.0 and the MariaDB JDBC driver, because Maven Central could not be reached from the build machine. The `pom.xml` is unchanged. Please run `mvn test` once on your side with `mysql-connector-j`.

## 8. This ZIP

Contains the backend that the module runs on (everything under `src/main/java` except the Swing `ui` package and `Main.java`), the config (`pom.xml`, `application.properties` with its placeholder password, `sql/`, `scripts/setup-db.ps1`, `run-server.cmd`, `run-tests.cmd`), and all tests. It does not contain the Swing frontend, the old top-level `main/` and `test/` copies, the SRS and schema documents, or the uploaded resume file in the original project's `resumes/` folder. The original project is not modified.

Start the server with `run-server.cmd` (it runs `com.placement.sockets.Server`). The `pom.xml` is the original one, so its jar settings still name `com.placement.Main`, the Swing launcher that is not in this ZIP.

Set the real database password with the `DB_PASSWORD` environment variable. `sql/02_seed.sql` holds demo accounts only (password `pass123`).
