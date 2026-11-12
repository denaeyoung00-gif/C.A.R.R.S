package carrs.repository;

import carrs.model.*;
import java.util.*;

public class InMemoryCarrsRepository implements CarrsRepository {
    private final Map<Integer, Applicant> applicants = new HashMap<>();
    private final Map<Integer, Programme> programmes = new LinkedHashMap<>();
    private final Map<Integer, Enrolment> enrolments = new LinkedHashMap<>();
    private final List<AuditEntry> audit = new ArrayList<>();
    private int nextEnrolmentId = 1000;
    public boolean failOnSave = false;   // lets a test simulate a database crash

    public void add(Applicant a) { applicants.put(a.getId(), a); }
    public void add(Programme p) { programmes.put(p.getId(), p); }
    public void add(Enrolment e) { enrolments.put(e.getId(), e); }

    @Override public Applicant findApplicant(int id) { return applicants.get(id); }
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
    @Override public List<AuditEntry> findAuditEntries() { return new ArrayList<>(audit); }
    @Override public void saveTransfer(Enrolment source, Enrolment created, AuditEntry entry) {
        if (failOnSave) throw new RuntimeException("Simulated database failure");
        enrolments.get(source.getId()).setStatus(EnrolmentStatus.TRANSFERRED);
        created.setId(nextEnrolmentId++);
        enrolments.put(created.getId(), created.copy());
        audit.add(entry);
    }
}