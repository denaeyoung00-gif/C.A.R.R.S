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
  programme_id    INT PRIMARY KEY AUTO_INCREMENT,
  name            VARCHAR(100) NOT NULL UNIQUE,
  status          ENUM('UPCOMING','ACTIVE','COMPLETED') NOT NULL,
  capacity        INT NOT NULL DEFAULT 25,
  required_fields VARCHAR(255) NOT NULL DEFAULT ''
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

-- ===== sample data =====
INSERT INTO staff_member (name, role) VALUES
 ('Alexia James','OFFICE_MANAGER'),
 ('Sam Intern','OFFICE_ATTENDANT'),
 ('Office Staff','OFFICE_STAFF'),
 ('Dr Angela Brown-Burke','MEMBER_OF_PARLIAMENT');

INSERT INTO programme (name,status,capacity,required_fields) VALUES
 ('Tertiary Scholarship / Education Grant','ACTIVE',25,''),
 ('Seasonal Agricultural Workers Programme (SAWP)','ACTIVE',25,''),
 ('Emergency Assistance / Compassionate Grant','ACTIVE',25,''),
 ('Constituency Summer Employment Programme (SEP)','ACTIVE',25,'');

INSERT INTO applicant (name,household_size,contact_number) VALUES
 ('Jane Brown','4','876-555-0101'),
 ('Tom Clarke',NULL,NULL),
 ('Mary Smith','3','876-555-0103');

INSERT INTO enrolment (applicant_id,programme_id,status,enrolled_at) VALUES
 (1,1,'ACTIVE',NOW()),
 (2,3,'ACTIVE',NOW()),
 (3,4,'ACTIVE',NOW());
