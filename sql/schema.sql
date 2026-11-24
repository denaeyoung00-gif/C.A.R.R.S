-- C.A.R.R.S database: structure only. Run this FIRST, then demo_data.sql.
DROP DATABASE IF EXISTS carrs;
CREATE DATABASE carrs;
USE carrs;

CREATE TABLE staff_member (
  staff_id INT PRIMARY KEY AUTO_INCREMENT,
  name     VARCHAR(100) NOT NULL,
  role     ENUM('OFFICE_ATTENDANT','OFFICE_STAFF','OFFICE_MANAGER','MEMBER_OF_PARLIAMENT') NOT NULL
);

CREATE TABLE applicant (
  applicant_id   INT PRIMARY KEY AUTO_INCREMENT,
  name           VARCHAR(100) NOT NULL,
  household_size VARCHAR(10) NULL,
  contact_number VARCHAR(20) NULL
);

CREATE TABLE programme (
  programme_id      INT PRIMARY KEY AUTO_INCREMENT,
  name              VARCHAR(100) NOT NULL UNIQUE,
  status            ENUM('UPCOMING','ACTIVE','COMPLETED') NOT NULL,
  capacity          INT NOT NULL DEFAULT 25,
  required_fields   VARCHAR(255) NOT NULL DEFAULT '',
  provides_benefits BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE enrolment (
  enrolment_id     INT PRIMARY KEY AUTO_INCREMENT,
  applicant_id     INT NOT NULL,
  programme_id     INT NOT NULL,
  status           ENUM('ACTIVE','ON_HOLD','WITHDRAWN','COMPLETED','TRANSFERRED') NOT NULL,
  enrolled_at      DATETIME NOT NULL,
  transferred_from INT NULL,
  FOREIGN KEY (applicant_id)     REFERENCES applicant(applicant_id),
  FOREIGN KEY (programme_id)     REFERENCES programme(programme_id),
  FOREIGN KEY (transferred_from) REFERENCES enrolment(enrolment_id)
);

-- ===== Payment and Benefit Management =====
CREATE TABLE payment (
  payment_id       INT PRIMARY KEY AUTO_INCREMENT,
  applicant_id     INT NOT NULL,
  programme_id     INT NOT NULL,
  benefit_type     ENUM('TUITION_SUPPORT','EMERGENCY_GRANT','SUMMER_STIPEND','OTHER') NOT NULL,
  payment_method   ENUM('CHEQUE') NOT NULL,
  amount           DECIMAL(12,2) NOT NULL,
  scheduled_date   DATE NOT NULL,
  distributed_date DATE NULL,
  status           ENUM('PENDING','PROCESSED','COMPLETED','FAILED') NOT NULL,
  failure_reason   VARCHAR(255) NULL,
  payee_name       VARCHAR(150) NULL,
  verified         BOOLEAN NOT NULL DEFAULT FALSE,
  verified_by      INT NULL,
  verified_at      DATETIME NULL,
  recorded_by      INT NOT NULL,
  recorded_at      DATETIME NOT NULL,
  cheque_scan_path VARCHAR(500) NULL,
  FOREIGN KEY (applicant_id) REFERENCES applicant(applicant_id),
  FOREIGN KEY (programme_id) REFERENCES programme(programme_id),
  FOREIGN KEY (verified_by)  REFERENCES staff_member(staff_id),
  FOREIGN KEY (recorded_by)  REFERENCES staff_member(staff_id)
);

CREATE TABLE payment_change (
  change_id   INT PRIMARY KEY AUTO_INCREMENT,
  payment_id  INT NOT NULL,
  field_name  VARCHAR(50)  NOT NULL,
  old_value   VARCHAR(255) NULL,
  new_value   VARCHAR(255) NULL,
  reason      VARCHAR(255) NOT NULL,
  changed_by  INT NOT NULL,
  changed_at  DATETIME NOT NULL,
  FOREIGN KEY (payment_id) REFERENCES payment(payment_id),
  FOREIGN KEY (changed_by) REFERENCES staff_member(staff_id)
);

CREATE TABLE payment_audit (
  audit_id     INT PRIMARY KEY AUTO_INCREMENT,
  payment_id   INT NULL,
  action       VARCHAR(40)  NOT NULL,
  details      VARCHAR(500) NOT NULL,
  performed_by INT NOT NULL,
  performed_at DATETIME NOT NULL,
  FOREIGN KEY (payment_id)   REFERENCES payment(payment_id),
  FOREIGN KEY (performed_by) REFERENCES staff_member(staff_id)
);
