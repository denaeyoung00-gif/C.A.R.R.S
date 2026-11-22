package carrs.repository;

import carrs.model.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class MySqlCarrsRepository implements CarrsRepository {

    // ============ shared methods (unchanged apart from findAllApplicants) ============
    @Override public Applicant findApplicant(int id) {
        String sql = "SELECT * FROM applicant WHERE applicant_id = ?";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? toApplicant(rs) : null; }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override public List<Applicant> findAllApplicants() {
        List<Applicant> out = new ArrayList<>();
        try (Connection c = Database.connect(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM applicant ORDER BY name")) {
            while (rs.next()) out.add(toApplicant(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return out;
    }

    @Override public List<StaffMember> findAllStaff() {
        List<StaffMember> out = new ArrayList<>();
        try (Connection c = Database.connect(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM staff_member ORDER BY staff_id")) {
            while (rs.next())
                out.add(new StaffMember(rs.getInt("staff_id"), rs.getString("name"), Role.valueOf(rs.getString("role"))));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return out;
    }

    @Override public Programme findProgramme(int id) {
        String sql = "SELECT * FROM programme WHERE programme_id = ?";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? toProgramme(rs) : null; }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override public Enrolment findEnrolment(int id) {
        String sql = "SELECT * FROM enrolment WHERE enrolment_id = ?";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? toEnrolment(rs) : null; }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override public List<Enrolment> findEnrolmentsForApplicant(int applicantId) {
        String sql = "SELECT * FROM enrolment WHERE applicant_id = ?";
        List<Enrolment> out = new ArrayList<>();
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, applicantId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(toEnrolment(rs)); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return out;
    }

    @Override public List<Programme> findActiveProgrammes() {
        List<Programme> out = new ArrayList<>();
        try (Connection c = Database.connect(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM programme WHERE status = 'ACTIVE'")) {
            while (rs.next()) out.add(toProgramme(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return out;
    }

    @Override public int countLiveEnrolments(int programmeId) {
        String sql = "SELECT COUNT(*) FROM enrolment WHERE programme_id = ? AND status IN ('ACTIVE','ON_HOLD')";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, programmeId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    // ============ Payment & Benefit Management ============
    private static final String INSERT_PAYMENT =
            "INSERT INTO payment (applicant_id, programme_id, benefit_type, payment_method, amount, scheduled_date, "
          + "distributed_date, status, failure_reason, payee_name, verified, verified_by, verified_at, "
          + "recorded_by, recorded_at, cheque_scan_path) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    /** One transaction: the new payment and its audit line are saved together, or neither is. */
    @Override public void savePayment(Payment p, PaymentAudit audit) {
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(INSERT_PAYMENT, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, p.getApplicantId());
                    ps.setInt(2, p.getProgrammeId());
                    ps.setString(3, p.getBenefitType().name());
                    ps.setString(4, p.getPaymentMethod().name());
                    ps.setBigDecimal(5, p.getAmount());
                    ps.setDate(6, java.sql.Date.valueOf(p.getScheduledDate()));
                    setDate(ps, 7, p.getDistributedDate());
                    ps.setString(8, p.getStatus().name());
                    ps.setString(9, p.getFailureReason());
                    ps.setString(10, p.getPayeeName());
                    ps.setBoolean(11, p.isVerified());
                    setInteger(ps, 12, p.getVerifiedBy());
                    setDateTime(ps, 13, p.getVerifiedAt());
                    ps.setInt(14, p.getRecordedBy());
                    ps.setTimestamp(15, Timestamp.valueOf(p.getRecordedAt()));
                    ps.setString(16, p.getChequeScanPath());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) p.setId(keys.getInt(1)); }
                }
                if (audit != null) { audit.setPaymentId(p.getId()); insertAudit(c, audit); }
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    /** One transaction: the payment update, its change records and its audit line succeed together, or none do. */
    @Override public void updatePayment(Payment p, List<PaymentChange> changes, PaymentAudit audit) {
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE payment SET benefit_type=?, payment_method=?, amount=?, scheduled_date=?, "
                      + "distributed_date=?, status=?, failure_reason=?, payee_name=?, verified=?, verified_by=?, "
                      + "verified_at=?, cheque_scan_path=? WHERE payment_id=?")) {
                    ps.setString(1, p.getBenefitType().name());
                    ps.setString(2, p.getPaymentMethod().name());
                    ps.setBigDecimal(3, p.getAmount());
                    ps.setDate(4, java.sql.Date.valueOf(p.getScheduledDate()));
                    setDate(ps, 5, p.getDistributedDate());
                    ps.setString(6, p.getStatus().name());
                    ps.setString(7, p.getFailureReason());
                    ps.setString(8, p.getPayeeName());
                    ps.setBoolean(9, p.isVerified());
                    setInteger(ps, 10, p.getVerifiedBy());
                    setDateTime(ps, 11, p.getVerifiedAt());
                    ps.setString(12, p.getChequeScanPath());
                    ps.setInt(13, p.getId());
                    ps.executeUpdate();
                }
                for (PaymentChange ch : changes) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO payment_change (payment_id, field_name, old_value, new_value, reason, "
                          + "changed_by, changed_at) VALUES (?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                        ps.setInt(1, ch.getPaymentId());
                        ps.setString(2, ch.getFieldName());
                        ps.setString(3, ch.getOldValue());
                        ps.setString(4, ch.getNewValue());
                        ps.setString(5, ch.getReason());
                        ps.setInt(6, ch.getChangedBy());
                        ps.setTimestamp(7, Timestamp.valueOf(ch.getChangedAt()));
                        ps.executeUpdate();
                        try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) ch.setId(keys.getInt(1)); }
                    }
                }
                if (audit != null) insertAudit(c, audit);
                c.commit();
            } catch (SQLException e) {
                c.rollback();   // undo everything if any step failed
                throw e;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override public void saveAudit(PaymentAudit audit) {
        try (Connection c = Database.connect()) { insertAudit(c, audit); }
        catch (SQLException e) { throw new RuntimeException(e); }
    }

    private static void insertAudit(Connection c, PaymentAudit a) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO payment_audit (payment_id, action, details, performed_by, performed_at) VALUES (?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            setInteger(ps, 1, a.getPaymentId());
            ps.setString(2, a.getAction());
            ps.setString(3, a.getDetails());
            ps.setInt(4, a.getPerformedBy());
            ps.setTimestamp(5, Timestamp.valueOf(a.getPerformedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) a.setId(keys.getInt(1)); }
        }
    }

    @Override public List<PaymentAudit> findAuditForApplicant(int applicantId) {
        String sql = "SELECT a.* FROM payment_audit a JOIN payment p ON p.payment_id = a.payment_id "
                   + "WHERE p.applicant_id = ? ORDER BY a.audit_id";
        List<PaymentAudit> out = new ArrayList<>();
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, applicantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int pid = rs.getInt("payment_id");
                    out.add(new PaymentAudit(rs.getInt("audit_id"), rs.wasNull() ? null : pid,
                            rs.getString("action"), rs.getString("details"), rs.getInt("performed_by"),
                            rs.getTimestamp("performed_at").toLocalDateTime()));
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return out;
    }

    @Override public Payment findPayment(int id) {
        String sql = "SELECT * FROM payment WHERE payment_id = ?";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? toPayment(rs) : null; }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override public List<Payment> findPaymentsByApplicant(int applicantId) {
        String sql = "SELECT * FROM payment WHERE applicant_id = ? ORDER BY scheduled_date DESC, payment_id DESC";
        List<Payment> out = new ArrayList<>();
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, applicantId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(toPayment(rs)); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return out;
    }

    @Override public Payment findLivePayment(int applicantId, int programmeId, BenefitType type, LocalDate date) {
        String sql = "SELECT * FROM payment WHERE applicant_id=? AND programme_id=? AND benefit_type=? "
                   + "AND scheduled_date=? AND status <> 'FAILED' LIMIT 1";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, applicantId);
            ps.setInt(2, programmeId);
            ps.setString(3, type.name());
            ps.setDate(4, java.sql.Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? toPayment(rs) : null; }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override public List<Payment> findCompletedPaymentsBetween(LocalDate from, LocalDate to) {
        String sql = "SELECT * FROM payment WHERE status = 'COMPLETED' AND distributed_date BETWEEN ? AND ?";
        List<Payment> out = new ArrayList<>();
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(from));
            ps.setDate(2, java.sql.Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(toPayment(rs)); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return out;
    }

    @Override public List<PaymentChange> findPaymentChanges(int paymentId) {
        String sql = "SELECT * FROM payment_change WHERE payment_id = ? ORDER BY change_id";
        List<PaymentChange> out = new ArrayList<>();
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, paymentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    out.add(new PaymentChange(rs.getInt("change_id"), rs.getInt("payment_id"),
                            rs.getString("field_name"), rs.getString("old_value"), rs.getString("new_value"),
                            rs.getString("reason"), rs.getInt("changed_by"),
                            rs.getTimestamp("changed_at").toLocalDateTime()));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return out;
    }

    // ============ helpers: turn rows into objects ============
    private Applicant toApplicant(ResultSet rs) throws SQLException {
        Applicant a = new Applicant(rs.getInt("applicant_id"), rs.getString("name"));
        a.with("household_size", rs.getString("household_size"));
        a.with("contact_number", rs.getString("contact_number"));
        return a;
    }

    private Programme toProgramme(ResultSet rs) throws SQLException {
        Programme p = new Programme(rs.getInt("programme_id"), rs.getString("name"),
                ProgrammeStatus.valueOf(rs.getString("status")), rs.getInt("capacity"));
        String req = rs.getString("required_fields");
        if (req != null && !req.isBlank())
            for (String f : req.split(",")) p.requires(f.trim());
        p.paysBenefits(rs.getBoolean("provides_benefits"));
        return p;
    }

    private Enrolment toEnrolment(ResultSet rs) throws SQLException {
        int from = rs.getInt("transferred_from");
        Integer fromId = rs.wasNull() ? null : from;
        return new Enrolment(rs.getInt("enrolment_id"), rs.getInt("applicant_id"),
                rs.getInt("programme_id"), EnrolmentStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("enrolled_at").toLocalDateTime(), fromId);
    }

    private Payment toPayment(ResultSet rs) throws SQLException {
        java.sql.Date dist = rs.getDate("distributed_date");
        int vb = rs.getInt("verified_by");
        Integer verifiedBy = rs.wasNull() ? null : vb;
        Timestamp vat = rs.getTimestamp("verified_at");
        return new Payment(rs.getInt("payment_id"), rs.getInt("applicant_id"), rs.getInt("programme_id"),
                BenefitType.valueOf(rs.getString("benefit_type")),
                PaymentMethod.valueOf(rs.getString("payment_method")), rs.getBigDecimal("amount"),
                rs.getDate("scheduled_date").toLocalDate(), dist == null ? null : dist.toLocalDate(),
                PaymentStatus.valueOf(rs.getString("status")), rs.getString("failure_reason"),
                rs.getString("payee_name"), rs.getBoolean("verified"), verifiedBy,
                vat == null ? null : vat.toLocalDateTime(),
                rs.getInt("recorded_by"), rs.getTimestamp("recorded_at").toLocalDateTime(),
                rs.getString("cheque_scan_path"));
    }

    private static void setDate(PreparedStatement ps, int i, LocalDate d) throws SQLException {
        if (d == null) ps.setNull(i, Types.DATE); else ps.setDate(i, java.sql.Date.valueOf(d));
    }
    private static void setDateTime(PreparedStatement ps, int i, LocalDateTime d) throws SQLException {
        if (d == null) ps.setNull(i, Types.TIMESTAMP); else ps.setTimestamp(i, Timestamp.valueOf(d));
    }
    private static void setInteger(PreparedStatement ps, int i, Integer v) throws SQLException {
        if (v == null) ps.setNull(i, Types.INTEGER); else ps.setInt(i, v);
    }
}
