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
            "INSERT INTO payment (applicant_id, programme_id, benefit_type, amount, scheduled_date, "
          + "distributed_date, status, failure_reason, payee_name, verified, verified_by, verified_at, "
          + "recorded_by, recorded_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    @Override public void savePayment(Payment p) {
        try (Connection c = Database.connect();
             PreparedStatement ps = c.prepareStatement(INSERT_PAYMENT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getApplicantId());
            ps.setInt(2, p.getProgrammeId());
            ps.setString(3, p.getBenefitType().name());
            ps.setBigDecimal(4, p.getAmount());
            ps.setDate(5, java.sql.Date.valueOf(p.getScheduledDate()));
            setDate(ps, 6, p.getDistributedDate());
            ps.setString(7, p.getStatus().name());
            ps.setString(8, p.getFailureReason());
            ps.setString(9, p.getPayeeName());
            ps.setBoolean(10, p.isVerified());
            setInteger(ps, 11, p.getVerifiedBy());
            setDateTime(ps, 12, p.getVerifiedAt());
            ps.setInt(13, p.getRecordedBy());
            ps.setTimestamp(14, Timestamp.valueOf(p.getRecordedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) p.setId(keys.getInt(1)); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    /** One transaction: the payment update and all its change records succeed together, or none do. */
    @Override public void updatePayment(Payment p, List<PaymentChange> changes) {
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE payment SET benefit_type=?, amount=?, scheduled_date=?, distributed_date=?, "
                      + "status=?, failure_reason=?, payee_name=?, verified=?, verified_by=?, verified_at=? "
                      + "WHERE payment_id=?")) {
                    ps.setString(1, p.getBenefitType().name());
                    ps.setBigDecimal(2, p.getAmount());
                    ps.setDate(3, java.sql.Date.valueOf(p.getScheduledDate()));
                    setDate(ps, 4, p.getDistributedDate());
                    ps.setString(5, p.getStatus().name());
                    ps.setString(6, p.getFailureReason());
                    ps.setString(7, p.getPayeeName());
                    ps.setBoolean(8, p.isVerified());
                    setInteger(ps, 9, p.getVerifiedBy());
                    setDateTime(ps, 10, p.getVerifiedAt());
                    ps.setInt(11, p.getId());
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
                c.commit();
            } catch (SQLException e) {
                c.rollback();   // undo everything if any step failed
                throw e;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
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
                BenefitType.valueOf(rs.getString("benefit_type")), rs.getBigDecimal("amount"),
                rs.getDate("scheduled_date").toLocalDate(), dist == null ? null : dist.toLocalDate(),
                PaymentStatus.valueOf(rs.getString("status")), rs.getString("failure_reason"),
                rs.getString("payee_name"), rs.getBoolean("verified"), verifiedBy,
                vat == null ? null : vat.toLocalDateTime(),
                rs.getInt("recorded_by"), rs.getTimestamp("recorded_at").toLocalDateTime());
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
