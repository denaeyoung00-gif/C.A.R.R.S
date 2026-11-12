package carrs.repository;

import carrs.model.*;
import java.sql.*;
import java.util.*;

public class MySqlCarrsRepository implements CarrsRepository {

    @Override public Applicant findApplicant(int id) {
        String sql = "SELECT * FROM applicant WHERE applicant_id = ?";
        try (Connection c = Database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Applicant a = new Applicant(rs.getInt("applicant_id"), rs.getString("name"));
                a.with("household_size", rs.getString("household_size"));
                a.with("contact_number", rs.getString("contact_number"));
                return a;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
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

    @Override public List<AuditEntry> findAuditEntries() {
        List<AuditEntry> out = new ArrayList<>();
        try (Connection c = Database.connect(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM transfer_audit ORDER BY audit_id")) {
            while (rs.next())
                out.add(new AuditEntry(rs.getInt("staff_id"),
                        rs.getTimestamp("transferred_at").toLocalDateTime(),
                        rs.getInt("source_programme"), rs.getInt("destination_programme"),
                        rs.getString("reason"), rs.getInt("applicant_id")));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return out;
    }

    /** One transaction: all three writes succeed together, or none do. */
    @Override public void saveTransfer(Enrolment source, Enrolment created, AuditEntry audit) {
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE enrolment SET status = 'TRANSFERRED' WHERE enrolment_id = ?")) {
                    ps.setInt(1, source.getId());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO enrolment (applicant_id, programme_id, status, enrolled_at, transferred_from) "
                                + "VALUES (?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, created.getApplicantId());
                    ps.setInt(2, created.getProgrammeId());
                    ps.setString(3, created.getStatus().name());
                    ps.setTimestamp(4, Timestamp.valueOf(created.getEnrolledAt()));
                    ps.setInt(5, created.getTransferredFromEnrolmentId());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) created.setId(keys.getInt(1)); }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO transfer_audit (staff_id, transferred_at, source_programme, "
                                + "destination_programme, reason, applicant_id) VALUES (?,?,?,?,?,?)")) {
                    ps.setInt(1, audit.getStaffId());
                    ps.setTimestamp(2, Timestamp.valueOf(audit.getTimestamp()));
                    ps.setInt(3, audit.getSourceProgrammeId());
                    ps.setInt(4, audit.getDestinationProgrammeId());
                    ps.setString(5, audit.getReason());
                    ps.setInt(6, audit.getApplicantId());
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();   // undo everything if any step failed
                throw e;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
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
}