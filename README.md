# Clinic Appointment System

A desktop application for booking and managing doctor appointments, integrated with each doctor's working schedule. Built with **Java Swing** (FlatLaf look and feel) and **MySQL**.

Patients see only the time slots a doctor is actually available. Doctors set their own weekly hours and days off, and approve or decline booking requests. Reception/admin staff manage everything from one place. Double-booking is prevented at the database level.

## Features

### Patient
- Sign up for an account from the login screen
- Pick a doctor and a date, then choose from live-updating free time slots
- Request an appointment (starts as *Pending* until the doctor/staff approves it)
- View and cancel own appointments
- Edit own profile (contact, address, emergency contact, insurance, etc.)

### Doctor
- Dashboard with today's appointments, completed count, next appointment
- Approve, cancel, or mark appointments as completed (single or bulk)
- Set weekly working hours, appointment length (10-60 min), and days off
- Edit own professional profile

### Admin / Reception
- Dashboard with totals for today, the next 7 days, doctors, and patients
- View and manage all appointments (search, filter by status/date, bulk actions)
- Book appointments on behalf of any patient
- Add doctors and patients, edit their profiles, edit a doctor's schedule
- Reset any user's password

## Appointment workflow

```
PENDING ──approve──▶ APPROVED ──complete──▶ COMPLETED
   │                    │
   └────────cancel──────┴──▶ CANCELLED
```

- Pending and approved appointments **occupy** the slot. Cancelled and completed ones free it.
- A patient may have only **one active appointment per day**.
- Past appointments cannot be cancelled.

## How availability works

Available slots are generated in real time (`SlotService`) from:

1. the doctor's weekly working hours (`schedule_rules`),
2. the chosen appointment length (`slot_minutes`),
3. the doctor's days off (`schedule_exceptions`),
4. existing pending/approved bookings.

Slots in the past and on days off are hidden. As a second line of defense, a `UNIQUE` key on `(doctor_id, appt_date, start_time, active_slot)` rejects a double booking even if two users click at the same moment.

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| UI | Swing + [FlatLaf](https://www.formdev.com/flatlaf/) 3.5.4 |
| Database | MySQL 8+ via `mysql-connector-j` 8.4.0 |
| Build | Maven |
| Passwords | Salted PBKDF2-HMAC-SHA256 (120,000 iterations), no external library |

## Project structure

```
ClinicAppointments/
├── pom.xml
├── database/
│   └── schema.sql              # Database, tables, and sample data
└── src/main/
    ├── java/com/clinic/
    │   ├── Main.java           # Entry point, creates default admin on first run
    │   ├── dao/                # Database access (User, Doctor, Patient, Schedule, Appointment)
    │   ├── db/Database.java    # Connection factory (reads db.properties)
    │   ├── model/              # Records: User, Doctor, Patient, Appointment, Slot, ...
    │   ├── service/            # Passwords (hashing), SlotService (availability + booking)
    │   └── ui/                 # Swing screens, dialogs, and reusable components
    └── resources/
        └── db.properties       # Database connection settings
```

## Requirements

- JDK 17 or newer
- Maven 3.8+ (or NetBeans, which bundles it)
- MySQL Server 8.0+

## Setup

### 1. Create the database

```bash
mysql -u root -p < database/schema.sql
```

This creates the `appointment_db` database, all tables, and loads sample data (accounts, doctors, patients, schedules, and appointments in every status) so the app has something to show on first launch.

### 2. Configure the connection

Edit `src/main/resources/db.properties` to match your MySQL server:

```properties
db.url=jdbc:mysql://localhost:3306/appointment_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=your_password
```

### 3. Run

**NetBeans:** open the project folder, then press **Run** (main class: `com.clinic.Main`).

**Command line:**

```bash
mvn compile exec:java
```

## Sample accounts

All sample accounts use the password **`password123`**.

| Role | Username | Name | Specialization / notes |
|---|---|---|---|
| Admin | `admin` | Maria Santos | Reception / administrator |
| Doctor | `dr.smith` | John Smith | Cardiology (30-min slots) |
| Doctor | `dr.chen` | Wei Chen | Pediatrics (20-min slots) |
| Doctor | `dr.patel` | Priya Patel | Dermatology (45-min slots) |
| Patient | `alice`, `bob`, `carol`, `daniel` | - | Sample patients with appointment history |

Sample appointment dates are generated relative to the current date, so there are always past and upcoming bookings to look at. `dr.smith` also has a sample day off one week from today.

> If the `users` table has no admin at all, the app creates one on first run (`admin` / `admin123`). With the sample data loaded this does not happen. Change all passwords before using the system for anything real.

New patients can also sign up from the login screen, and the admin can add doctors and set their working hours.

## Database design

| Table | Purpose |
|---|---|
| `users` | Login accounts for all roles (`ADMIN`, `DOCTOR`, `PATIENT`) |
| `doctors` | Doctor profile, linked 1:1 to a `users` row; holds `slot_minutes` |
| `patients` | Patient profile, linked 1:1 to a `users` row |
| `schedule_rules` | Weekly recurring working hours per doctor (1 = Monday ... 7 = Sunday) |
| `schedule_exceptions` | Days off per doctor |
| `appointments` | Bookings with status and the `active_slot` unique constraint |

## Notes and limitations

- Changing working hours or adding a day off does **not** cancel existing bookings; they stay until cancelled manually.
- Passwords are hashed, but the database connection itself is configured in plain text in `db.properties`. Do not commit real credentials.
- This is a school project, not a production medical system. It has no audit log and no email/SMS notifications.