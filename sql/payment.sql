USE carrs;

CREATE TABLE payment (
  payment_id       INT PRIMARY KEY AUTO_INCREMENT,
  applicant_id     INT NOT NULL,
  programme_id     INT NOT NULL,
  benefit_type     ENUM('TUITION_SUPPORT','EMERGENCY_GRANT','SUMMER_STIPEND','OTHER') NOT NULL,
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

-- sample data for the demo
INSERT INTO payment (applicant_id, programme_id, benefit_type, amount, scheduled_date, distributed_date,
                     status, payee_name, recorded_by, recorded_at) VALUES
 (1, 1, 'TUITION_SUPPORT', 85000.00, '2026-11-10', '2026-11-10', 'COMPLETED',
  'University of the West Indies', 3, NOW()),
 (2, 3, 'EMERGENCY_GRANT', 30000.00, '2026-11-25', NULL, 'PENDING',
  'Funeral home (quotation on file)', 3, NOW());
