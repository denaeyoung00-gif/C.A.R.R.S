package carrs.model;

import java.time.LocalDateTime;

/** A constituent's application to a programme, with the decision made on it (used by Appeals & Disputes). */
public class Application {
    private final int id;
    private final int applicantId;
    private final String applicantName;
    private final int programmeId;
    private final String programmeName;
    private String status;                 // Approved / Rejected / Disqualified
    private LocalDateTime decisionDate;
    private boolean locked;
    private LocalDateTime archivedUntil;
    private boolean enrollmentTaskCreated;

    public Application(int id, int applicantId, String applicantName, int programmeId, String programmeName,
                       String status, LocalDateTime decisionDate, boolean locked,
                       LocalDateTime archivedUntil, boolean enrollmentTaskCreated) {
        this.id = id; this.applicantId = applicantId; this.applicantName = applicantName;
        this.programmeId = programmeId; this.programmeName = programmeName;
        this.status = status; this.decisionDate = decisionDate; this.locked = locked;
        this.archivedUntil = archivedUntil; this.enrollmentTaskCreated = enrollmentTaskCreated;
    }

    public int getId() { return id; }
    public int getApplicantId() { return applicantId; }
    public String getApplicantName() { return applicantName; }
    public int getProgrammeId() { return programmeId; }
    public String getProgrammeName() { return programmeName; }
    public String getStatus() { return status; }
    public LocalDateTime getDecisionDate() { return decisionDate; }
    public boolean isLocked() { return locked; }
    public LocalDateTime getArchivedUntil() { return archivedUntil; }
    public boolean isEnrollmentTaskCreated() { return enrollmentTaskCreated; }

    public void setStatus(String status) { this.status = status; }
    public void setDecisionDate(LocalDateTime d) { this.decisionDate = d; }
    public void setLocked(boolean locked) { this.locked = locked; }
    public void setArchivedUntil(LocalDateTime d) { this.archivedUntil = d; }
    public void setEnrollmentTaskCreated(boolean b) { this.enrollmentTaskCreated = b; }
}
