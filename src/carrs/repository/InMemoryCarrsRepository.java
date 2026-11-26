package carrs.repository;

import carrs.model.*;
import java.time.LocalDate;
import java.util.*;

/** Fake repository used by the JUnit tests, so tests run without MySQL. */
public class InMemoryCarrsRepository implements CarrsRepository {
    private final Map<Integer, Applicant> applicants = new LinkedHashMap<>();
    private final Map<Integer, Programme> programmes = new LinkedHashMap<>();
    private final Map<Integer, Enrolment> enrolments = new LinkedHashMap<>();
    private final Map<Integer, Payment> payments = new LinkedHashMap<>();
    private final List<PaymentChange> changes = new ArrayList<>();
    private final List<PaymentAudit> audits = new ArrayList<>();
    private int nextPaymentId = 1;
    private int nextAuditId = 1;
    private int nextChangeId = 1;
    public boolean failOnSave = false;   // lets a test simulate a database crash

    private final List<StaffMember> staffList = new ArrayList<>();
    public void add(StaffMember s) { staffList.add(s); }
    public void add(Applicant a) { applicants.put(a.getId(), a); }
    public void add(Programme p) { programmes.put(p.getId(), p); }
    public void add(Enrolment e) { enrolments.put(e.getId(), e); }

    @Override public Applicant findApplicant(int id) { return applicants.get(id); }
    @Override public List<Applicant> findAllApplicants() { return new ArrayList<>(applicants.values()); }
    @Override public List<StaffMember> findAllStaff() { return new ArrayList<>(staffList); }
    @Override public Programme findProgramme(int id) { return programmes.get(id); }
    @Override public Enrolment findEnrolment(int id) {
        Enrolment e = enrolments.get(id);
        return e == null ? null : e.copy();
    }
    @Override public List<Enrolment> findEnrolmentsForApplicant(int applicantId) {
        List<Enrolment> out = new ArrayList<>();
        for (Enrolment e : enrolments.values())
            if (e.getApplicantId() == applicantId) out.add(e.copy());
        return out;
    }
    @Override public List<Programme> findActiveProgrammes() {
        List<Programme> out = new ArrayList<>();
        for (Programme p : programmes.values()) if (p.isActive()) out.add(p);
        return out;
    }
    @Override public int countLiveEnrolments(int programmeId) {
        int n = 0;
        for (Enrolment e : enrolments.values())
            if (e.getProgrammeId() == programmeId && e.isLive()) n++;
        return n;
    }

    // ---- payments
    @Override public void savePayment(Payment p, PaymentAudit audit) {
        if (failOnSave) throw new RuntimeException("Simulated database failure");
        p.setId(nextPaymentId++);
        payments.put(p.getId(), p.copy());
        if (audit != null) { audit.setPaymentId(p.getId()); audit.setId(nextAuditId++); audits.add(audit); }
    }
    @Override public void updatePayment(Payment p, List<PaymentChange> newChanges, PaymentAudit audit) {
        if (failOnSave) throw new RuntimeException("Simulated database failure");
        payments.put(p.getId(), p.copy());
        for (PaymentChange c : newChanges) { c.setId(nextChangeId++); changes.add(c); }
        if (audit != null) { audit.setId(nextAuditId++); audits.add(audit); }
    }
    @Override public void saveAudit(PaymentAudit audit) {
        if (failOnSave) throw new RuntimeException("Simulated database failure");
        audit.setId(nextAuditId++); audits.add(audit);
    }
    @Override public List<PaymentAudit> findAuditForApplicant(int applicantId) {
        List<PaymentAudit> out = new ArrayList<>();
        for (PaymentAudit a : audits) {
            Payment p = a.getPaymentId() == null ? null : payments.get(a.getPaymentId());
            if (p != null && p.getApplicantId() == applicantId) out.add(a);
        }
        return out;
    }
    @Override public Payment findPayment(int id) {
        Payment p = payments.get(id);
        return p == null ? null : p.copy();
    }
    @Override public List<Payment> findPaymentsByApplicant(int applicantId) {
        List<Payment> out = new ArrayList<>();
        for (Payment p : payments.values()) if (p.getApplicantId() == applicantId) out.add(p.copy());
        out.sort(Comparator.comparing(Payment::getScheduledDate).reversed());
        return out;
    }
    @Override public Payment findLivePayment(int applicantId, int programmeId, BenefitType type, LocalDate date) {
        for (Payment p : payments.values())
            if (p.getApplicantId() == applicantId && p.getProgrammeId() == programmeId
                    && p.getBenefitType() == type && p.getScheduledDate().equals(date)
                    && p.getStatus() != PaymentStatus.FAILED) return p.copy();
        return null;
    }
    @Override public List<Payment> findCompletedPaymentsBetween(LocalDate from, LocalDate to) {
        List<Payment> out = new ArrayList<>();
        for (Payment p : payments.values())
            if (p.getStatus() == PaymentStatus.COMPLETED && p.getDistributedDate() != null
                    && !p.getDistributedDate().isBefore(from) && !p.getDistributedDate().isAfter(to))
                out.add(p.copy());
        return out;
    }
    @Override public List<PaymentChange> findPaymentChanges(int paymentId) {
        List<PaymentChange> out = new ArrayList<>();
        for (PaymentChange c : changes) if (c.getPaymentId() == paymentId) out.add(c);
        return out;
    }

    // ---- Appeals & Disputes (in memory, for tests)
    private final Map<Integer, Application> applications = new LinkedHashMap<>();
    private final Map<String, AppealRecord> appeals = new LinkedHashMap<>();
    public void add(Application a) { applications.put(a.getId(), a); }
    @Override public List<Application> findApplications() { return new ArrayList<>(applications.values()); }
    @Override public void saveAppeal(AppealRecord a) {
        appeals.put(a.getCadn(), a);
        Application app = applications.get(a.getApplicationId());
        if (app != null) app.setLocked(true);
    }
    @Override public void saveAppealDecision(AppealRecord a, Application app) {
        appeals.put(a.getCadn(), a);
        applications.put(app.getId(), app);
    }
    @Override public List<AppealRecord> findAppeals() { return new ArrayList<>(appeals.values()); }

    // ---- Registration & Approval of programme applications
    private final Map<Integer, ProgrammeApplication> programmeApplications = new LinkedHashMap<>();
    private int nextApplicantId = 1000;
    private int nextProgrammeApplicationId = 1;

    @Override public List<Programme> findAllProgrammes() { return new ArrayList<>(programmes.values()); }
    @Override public void createApplicantAndApplication(Applicant a, ProgrammeApplication app, StaffMember by) {
        if (failOnSave) throw new RuntimeException("database down");
        Applicant saved = new Applicant(nextApplicantId++, a.getName());
        applicants.put(saved.getId(), saved);
        app.setId(nextProgrammeApplicationId++);
        programmeApplications.put(app.getId(), app);
    }
    @Override public List<ProgrammeApplication> findApplicationsByStatus(ApplicationStatus status) {
        List<ProgrammeApplication> out = new ArrayList<>();
        for (ProgrammeApplication p : programmeApplications.values()) if (p.getStatus() == status) out.add(p);
        return out;
    }
    @Override public void decideApplication(int applicationId, ApplicationStatus decision, String reason, StaffMember by) {
        ProgrammeApplication p = programmeApplications.get(applicationId);
        if (p == null || p.getStatus() != ApplicationStatus.COMPLETED)
            throw new IllegalStateException("That application was not found or has already been decided.");
        if (decision == ApplicationStatus.APPROVED) {
            Programme prog = programmes.get(p.getProgrammeId());
            if (prog != null && countLiveEnrolments(prog.getId()) >= prog.getCapacity())
                throw new IllegalStateException("This programme is full, so the application cannot be approved.");
        }
        p.setStatus(decision);
        p.setRejectionReason(decision == ApplicationStatus.REJECTED ? reason : null);
        if (decision == ApplicationStatus.APPROVED) {
            boolean already = false;
            for (Enrolment e : enrolments.values())
                if (e.getApplicantId() == p.getApplicantId() && e.getProgrammeId() == p.getProgrammeId() && e.isLive()) already = true;
            if (!already) {
                int id = enrolments.keySet().stream().max(Integer::compare).orElse(0) + 1;
                enrolments.put(id, new Enrolment(id, p.getApplicantId(), p.getProgrammeId(), EnrolmentStatus.ACTIVE,
                        java.time.LocalDateTime.now(), null));
            }
        }
    }

    @Override public List<EnrolmentRow> findEnrolmentRows() {
        List<EnrolmentRow> out = new ArrayList<>();
        for (Enrolment e : enrolments.values()) {
            Applicant a = applicants.get(e.getApplicantId());
            Programme pr = programmes.get(e.getProgrammeId());
            out.add(new EnrolmentRow(e.getId(), e.getApplicantId(), e.getProgrammeId(),
                    a == null ? "?" : a.getName(), a == null ? null : a.getDetail("trn"),
                    pr == null ? "?" : pr.getName(), e.getStatus(), a == null ? null : a.getDetail("address"),
                    a == null ? null : a.getDetail("contact_number"), e.getEnrolledAt().toLocalDate()));
        }
        java.util.Collections.reverse(out);
        return out;
    }

    @Override public void updateEnrolmentRecord(int enrolmentId, EnrolmentStatus status, String phone, String address) {
        Enrolment e = enrolments.get(enrolmentId);
        if (e == null) throw new IllegalStateException("No enrolment found with ID " + enrolmentId);
        e.setStatus(status);
        Applicant a = applicants.get(e.getApplicantId());
        if (a != null) { a.with("contact_number", phone); a.with("address", address); }
    }

    // ---- Programme Management (in memory)
    private final Map<Integer, LocalDate[]> programmeDates = new HashMap<>();

    @Override public List<ProgrammeDetails> findProgrammeDetails() {
        List<ProgrammeDetails> out = new ArrayList<>();
        for (Programme p : programmes.values()) {
            LocalDate[] d = programmeDates.get(p.getId());
            out.add(new ProgrammeDetails(p.getId(), p.getName(), d == null ? null : d[0], d == null ? null : d[1],
                    p.getCapacity(), countLiveEnrolments(p.getId()), p.getStatus()));
        }
        return out;
    }
    private static ProgrammeStatus statusOf(LocalDate s, LocalDate f) { return ProgrammeDetails.statusFor(s, f, LocalDate.now()); }
    @Override public void addProgramme(String name, LocalDate start, LocalDate finish, int capacity) {
        for (Programme p : programmes.values())
            if (p.getName().equalsIgnoreCase(name)) throw new IllegalStateException("A programme called \"" + name + "\" already exists.");
        int id = programmes.keySet().stream().max(Integer::compare).orElse(0) + 1;
        programmes.put(id, new Programme(id, name, statusOf(start, finish), capacity));
        programmeDates.put(id, new LocalDate[]{start, finish});
    }
    @Override public void editProgramme(int id, String name, LocalDate start, LocalDate finish, int capacity) {
        if (!programmes.containsKey(id)) throw new IllegalStateException("That programme no longer exists.");
        for (Programme p : programmes.values())
            if (p.getId() != id && p.getName().equalsIgnoreCase(name)) throw new IllegalStateException("A programme called \"" + name + "\" already exists.");
        programmes.put(id, new Programme(id, name, statusOf(start, finish), capacity));
        programmeDates.put(id, new LocalDate[]{start, finish});
    }
    @Override public void removeProgramme(int id) {
        if (countLiveEnrolments(id) > 0)
            throw new IllegalStateException("This programme already has enrolments, so it cannot be deleted.");
        programmes.remove(id); programmeDates.remove(id);
    }
}
