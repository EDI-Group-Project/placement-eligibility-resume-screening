# Placement Eligibility and Resume Screening Portal

## 1. Project Overview

The **Placement Eligibility and Resume Screening Portal** is a client-server based placement management system developed using **Java TCP Socket Programming, JDBC, and MySQL**.

The main purpose of the system is to automate the college placement process by allowing students to manage their profiles and resumes, while Placement Officers and Company HR can manage recruitment drives, check candidate eligibility, search candidates, and perform placement-related activities.

The system follows a centralized client-server architecture where multiple clients communicate with a TCP server. The server processes client requests, performs business logic, communicates with the database using JDBC, and sends the result back to the client.

---

## 2. Objectives

The main objectives of the project are:

- To provide a centralized placement management system.
- To allow students to register and manage their academic profiles.
- To provide secure login and authentication.
- To allow students to upload and manage resume information.
- To allow companies to define eligibility criteria.
- To automatically identify eligible students.
- To provide candidate search functionality.
- To support placement-related notifications.
- To generate placement-related information and reports.
- To demonstrate TCP socket-based client-server communication.
- To support multiple clients using a multi-threaded server.
- To maintain student and company information using MySQL.

---

## 3. Features

### Student

- Student Registration
- Student Login
- Profile Management
- Academic Information Management
- Skills Management
- Resume Management
- View Placement Opportunities
- View Eligibility Status
- Receive Notifications

### Company HR

- Company Login
- Manage Recruitment Information
- Define Eligibility Criteria
- View Eligible Candidates
- Search Candidates
- Resume Screening

### Placement Officer / TPO

- Manage Student Records
- Manage Company Information
- Manage Placement Drives
- Search Candidates
- Check Eligible Students
- Send Notifications
- View Placement Statistics

### Administrator

- Manage Users
- Monitor System
- Manage Database
- Monitor Server

---

## 4. Technology Stack

| Technology | Purpose |
|---|---|
| Java | Application Development |
| Java Swing | Frontend / GUI |
| TCP Socket Programming | Client-Server Communication |
| MySQL | Database |
| JDBC | Database Connectivity |
| Object-Oriented Programming | Application Design |
| Multi-threading | Multiple Client Handling |
| IntelliJ IDEA / Eclipse | Development Environment |
| Git | Version Control |

---

## 5. System Architecture

The project follows a **Client-Server Architecture**.

```text
                         +----------------------+
                         |      MySQL Database  |
                         +----------^-----------+
                                    |
                                   JDBC
                                    |
                         +----------+-----------+
                         |      TCP SERVER      |
                         |                      |
                         |    ClientHandler     |
                         |          |           |
                         |    Backend Services  |
                         +----------^-----------+
                                    |
                              TCP Socket
                                    |
              +---------------------+---------------------+
              |                     |                     |
              v                     v                     v
      +---------------+     +---------------+     +---------------+
      | Student Client|     |  TPC / TPO    |     | Company Client|
      |               |     |    Client     |     |               |
      +---------------+     +---------------+     +---------------+
