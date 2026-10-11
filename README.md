# Placement Eligibility and Resume Screening Portal

A Java 17 + Swing + TCP sockets + MySQL application for placement eligibility, recruitment, and notification workflows.

> **Integration scope:** This version removes the application's dependency on the `MyEDI` folder and places the JDBC/database, service, socket, security, and test layers in the main Maven project.

## 1. Architecture

```text
Swing UI
   |
   | TCP socket requests
   v
Socket Server :5000
   |
   v
ClientHandler
   |
   +--> AuthService --------> FacultyDAO / CompanyDAO / StudentDAO
   |
   +--> StudentService -----> StudentDAO / EligibilityDAO
   |
   +--> JobService ---------> JobDAO / CompanyDAO
   |
   +--> NotificationService -> NotificationDAO / StudentDAO
   |
   v
JDBC
   |
   v
MySQL: placement_portal
```

### Project structure

```text
placement-eligibility-resume-screening-main/
├── pom.xml
├── README.md
├── run-server.cmd
├── run-client.cmd
├── sql/
│   ├── 01_schema.sql
│   └── 02_seed.sql
├── scripts/
│   └── setup-db.ps1
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/placement/
│   │   │       ├── Main.java
│   │   │       ├── config/AppConfig.java
│   │   │       ├── database/
│   │   │       ├── models/
│   │   │       ├── security/PasswordHasher.java
│   │   │       ├── services/
│   │   │       ├── sockets/
│   │   │       ├── ui/
│   │   │       └── validation/
│   │   └── resources/application.properties
│   └── test/
│       └── java/com/placement/integration/
│           ├── DatabaseIntegrationTest.java
│           ├── MultiClientTest.java
│           └── PasswordHasherTest.java
└── DATABASE SCHEMAS EDI.docx
```

`MyEDI/` is intentionally excluded from the runnable project.

---

# 2. Prerequisites on Windows

Install these before running the project:

1. **JDK 17 or newer**
2. **MySQL Server 8.x**
3. **Apache Maven 3.9+**
4. Git is optional but recommended.

Verify them in PowerShell:

```powershell
java -version
javac -version
mvn -version
mysql --version
```

Make sure `java`, `javac`, `mvn`, and `mysql` are available on `PATH`.

---

# 3. Database setup

## Option A - PowerShell setup script

Open PowerShell in the project root:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\scripts\setup-db.ps1
```

The script asks for the MySQL `root` password twice and executes:

```text
sql/01_schema.sql
sql/02_seed.sql
```

## Option B - Manual JDBC database setup

Open MySQL:

```powershell
mysql -u root -p
```

Then:

```sql
SOURCE C:/path/to/placement-eligibility-resume-screening-main/sql/01_schema.sql;
SOURCE C:/path/to/placement-eligibility-resume-screening-main/sql/02_seed.sql;
```

Use forward slashes in MySQL `SOURCE` paths on Windows.

Verify:

```sql
USE placement_portal;

SHOW TABLES;

SELECT COUNT(*) AS students FROM students;
SELECT COUNT(*) AS faculty FROM faculty;
SELECT COUNT(*) AS companies FROM company;
SELECT COUNT(*) AS jobs FROM job_postings;
```

The seeded database contains students, faculty, companies, eligibility rules, job postings, and notification/application/resume tables.

---

# 4. JDBC configuration

The application reads:

```text
src/main/resources/application.properties
```

Default settings:

```properties
db.url=jdbc:mysql://localhost:3306/placement_portal?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=change-me
```

**Do not commit a real database password to Git.**

For Windows, environment variables override the properties file.

PowerShell:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/placement_portal?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USER="root"
$env:DB_PASSWORD="YOUR_MYSQL_PASSWORD"
```

Check:

```powershell
echo $env:DB_URL
echo $env:DB_USER
```

The JDBC driver is declared in `pom.xml` as MySQL Connector/J.

---

# 5. How JDBC integration works

The database layer deliberately uses `PreparedStatement` instead of concatenating user input into SQL.

Example:

```java
try (Connection connection = DatabaseConnection.getConnection();
     PreparedStatement statement = connection.prepareStatement(
             "SELECT student_id, name FROM students WHERE email = ?")) {

    statement.setString(1, email);

    try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
            // map result
        }
    }
}
```

The central JDBC entry point is:

```text
com.placement.database.DatabaseConnection
```

It obtains the MySQL connection from:

```text
DB_URL / DB_USER / DB_PASSWORD
```

through `AppConfig`.

Transactions are used for multi-table operations such as:

```text
create eligibility rule
        +
create job posting
```

and:

```text
create notification
        +
create notification recipients
```

so partial writes do not remain after an error.

---

# 6. Build the complete project

From the project root:

```powershell
mvn clean test
```

This runs unit tests that do not require MySQL.

Build the application:

```powershell
mvn clean package
```

The runnable JAR is generated at:

```text
target\placement-eligibility-resume-screening-1.0.0.jar
```

---

# 7. Run database integration tests

Make sure MySQL is running and the schema + seed data have been loaded.

Set database environment variables:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/placement_portal?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USER="root"
$env:DB_PASSWORD="YOUR_MYSQL_PASSWORD"
```

Run:

```powershell
mvn -DRUN_DB_TESTS=true test
```

The integration tests verify:

- JDBC connection establishment
- seeded table contents
- database-backed authentication
- database-backed eligibility retrieval

---

# 8. Start the TCP server

Open **PowerShell Window 1**:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/placement_portal?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USER="root"
$env:DB_PASSWORD="YOUR_MYSQL_PASSWORD"

mvn clean compile
mvn exec:java -Dexec.mainClass=com.placement.sockets.Server
```

Expected output:

```text
Placement Eligibility Portal server listening on port 5000
Thread pool size: 15
```

The server handles one TCP client per pooled worker thread.

---

# 9. Start the Swing client

Open **PowerShell Window 2** in the same project directory:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/placement_portal?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USER="root"
$env:DB_PASSWORD="YOUR_MYSQL_PASSWORD"

mvn exec:java -Dexec.mainClass=com.placement.Main
```

Or:

```powershell
mvn clean package
java -jar target\placement-eligibility-resume-screening-1.0.0.jar
```

The client:

1. Checks JDBC availability.
2. Opens the Swing login page.
3. Sends `LOGIN` over TCP.
4. The server authenticates against MySQL.
5. A server-side session token is created.
6. The appropriate dashboard is opened.

---

# 10. Demo credentials

All seeded demo accounts use:

```text
Password: pass123
```

### Faculty

| Role | Email |
|---|---|
| TPO | amit@college.com |
| TPC | neha@college.com |
| Director | rajesh@college.com |
| Dean | pooja@college.com |
| Faculty | sanjay@college.com |

### Recruiters

| Company | Email |
|---|---|
| TCS | anita@tcs.com |
| Infosys | rahul@infosys.com |
| Accenture | kavita@accenture.com |
| Deloitte | vikas@deloitte.com |
| Wipro | sneha@wipro.com |

### Students

| Student | Email |
|---|---|
| Aarav Sharma | aarav@gmail.com |
| Priya Patil | priya@gmail.com |
| Rohan Mehta | rohan@gmail.com |
| Sneha Joshi | sneha@gmail.com |
| Vivek Shah | vivek@gmail.com |

These are demo credentials only.

---

# 11. TCP protocol currently implemented

The server supports:

```text
LOGIN
REGISTER_STUDENT
GET_JOBS
ADD_JOB
GET_STUDENTS
GET_ELIGIBLE_STUDENTS
SEND_NOTIFICATION
SEND_GENERAL_NOTIFICATION
GET_NOTIFICATIONS
GET_DASHBOARD_STATS
LOGOUT
```

Protected commands require the session token returned by `LOGIN`.

For example:

```text
LOGIN
TPC
neha@college.com
pass123
```

Successful response:

```text
SUCCESS|<session-token>|Neha Deshmukh
```

Then:

```text
GET_STUDENTS
<session-token>
```

---

# 12. Eligibility database flow

For a job:

```text
job_postings
      |
      +--> company
      |
      +--> eligibility
```

The eligibility rule stores:

```text
minimum CGPA
maximum backlogs
allowed departments
passing year
required skills
```

Eligible students are selected using JDBC and then checked against required skills before being returned to the TPC layer.

Already-notified students are excluded using:

```text
notification_recipients
        +
notifications.job_id
```

This prevents the same student from repeatedly appearing as pending for the same job.

---

# 13. Notification transaction flow

`SEND_NOTIFICATION` performs:

```text
validate session
       |
find job
       |
find student IDs
       |
BEGIN TRANSACTION
       |
insert notification
       |
insert notification recipients
       |
COMMIT
```

If any database write fails:

```text
ROLLBACK
```

so the notification log and recipients stay consistent.

---

# 14. Testing

## Unit tests

```powershell
mvn test
```

Current tests include password hashing and non-database behavior.

## JDBC integration tests

```powershell
mvn -DRUN_DB_TESTS=true test
```

## Concurrent socket test

After starting the server:

```powershell
mvn -q test-compile
mvn -q exec:java -Dexec.classpathScope=test -Dexec.mainClass=com.placement.integration.MultiClientTest
```

The concurrency test launches 15 clients and measures total and average round-trip latency.

---

# 15. Common Windows problems

## `mysql is not recognized`

Add the MySQL `bin` directory to `PATH`, for example:

```text
C:\Program Files\MySQL\MySQL Server 8.0\bin
```

Close and reopen PowerShell after changing PATH.

## `mvn is not recognized`

Install Apache Maven and set:

```text
MAVEN_HOME
```

then add:

```text
%MAVEN_HOME%\bin
```

to PATH.

## `Communications link failure`

Check:

```powershell
Get-Service *mysql*
```

and make sure the MySQL service is running.

Also verify:

```text
DB_URL
DB_USER
DB_PASSWORD
```

## `Address already in use: 5000`

Another process is using port 5000.

Find it:

```powershell
netstat -ano | findstr :5000
```

You can then stop the process or set another value:

```powershell
$env:SERVER_PORT="5001"
```

The client must then be configured with the same port.

## `Access denied for user 'root'`

The MySQL password supplied to `DB_PASSWORD` does not match the account.

---

# 16. Fresh installation from zero

Use this exact sequence on a new Windows machine:

```powershell
# 1. Go to the project folder
cd C:\path\to\placement-eligibility-resume-screening-main

# 2. Verify Java + Maven + MySQL
java -version
mvn -version
mysql --version

# 3. Configure JDBC
$env:DB_URL="jdbc:mysql://localhost:3306/placement_portal?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USER="root"
$env:DB_PASSWORD="YOUR_MYSQL_PASSWORD"

# 4. Create schema + seed data
Set-ExecutionPolicy -Scope Process Bypass
.\scripts\setup-db.ps1

# 5. Verify database integration
mvn -DRUN_DB_TESTS=true test

# 6. Build
mvn clean package

# 7. Terminal 1: start server
mvn exec:java -Dexec.mainClass=com.placement.sockets.Server

# 8. Terminal 2: start client
java -jar target\placement-eligibility-resume-screening-1.0.0.jar
```

Then log in using one of the seeded accounts above.

---

# 17. Security notes

- MySQL access is not hard-coded in Java source.
- SQL parameters use `PreparedStatement`.
- Passwords are stored using PBKDF2-HMAC-SHA256, not plaintext.
- Login creates a server-side session token.
- Protected socket commands require authentication.
- Multi-step notification and job writes use JDBC transactions.
- Demo passwords should be changed before deployment.
- For production, TLS should be added to the client/server connection and secrets should come from a proper secret store.

---

# 18. Important scope note

The original `DATABASE SCHEMAS EDI.docx` contained only the core four tables. The runnable integration adds the tables required by the existing placement workflow:

```text
job_postings
notifications
notification_recipients
resumes
applications
```

This avoids keeping notification/job data in Java memory and gives the integration layer a persistent MySQL source of truth.

The current resume/application tables provide the database foundation for the resume-screening portion; the supplied UI did not contain a completed resume parser/screening engine, so that logic is not fabricated here.

