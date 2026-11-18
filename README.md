# C.A.R.R.S - Constituency Assistance Registration & Record System

COMP2140 group project (Big Hero 6) for the Office of the MP, Dr Angela Brown-Burke.
This branch contains the **Programme Transfer** feature (FTR-GOV-005).

## What Programme Transfer does
An authorised staff member searches for an applicant, selects one of their
enrolments, chooses an active destination programme, enters a reason (at least
10 characters) and confirms. The system:
- keeps the original enrolment and marks it **Transferred**
- creates a new **Active** enrolment linked to the original
- records an audit entry (staff member, date/time, source, destination, reason)
- blocks the transfer, with a message and no changes saved, if the applicant is
  already in the destination or required information is missing

## Requirements
- JDK 21 or higher
- MySQL Server 8.0 and MySQL Workbench
- IntelliJ IDEA (or any Java IDE)
- Libraries (add them to the project):
  - JUnit 4.13.2 and hamcrest-core 1.3
  - mysql-connector-j 8.4.0

## Setup
1. **Create the database.** Open MySQL Workbench, open a new query tab, paste the
   whole of `sql/schema.sql`, and run it. This creates the `carrs` database,
   its tables and sample data.
2. **Add your settings.** Copy `db.properties.example` to `db.properties` in the
   project root and put your own MySQL password in it. Do not commit
   `db.properties`, because it is listed in `.gitignore`.
   ```
   url=jdbc:mysql://localhost:3306/carrs
   user=root
   password=YOUR_PASSWORD
   ```
   No quote marks and no spaces at the end of any line.
3. **Add the libraries** listed above (IntelliJ: File > Project Structure >
   Libraries).
4. Mark the `src` folder as **Sources Root**.
