import java.time.LocalDateTime;

public class ApplicationRecord {
    private final String applicationId;
    private final String applicantCitizenId;
    private final String programId;
    private String status;               // "Submitted", "Under Review", "Approved", "Rejected", "Disqualified"
    private LocalDateTime decisionDate;
    private String rejectionReason;
    private boolean isLocked;            // SR-4: Locked when appeal is lodged

    public ApplicationRecord(String applicationId, String applicantCitizenId, String programId, String status, LocalDateTime decisionDate) {
        this.applicationId = applicationId;
        this.applicantCitizenId = applicantCitizenId;
        this.programId = programId;
        this.status = status;
        this.decisionDate = decisionDate;
        this.isLocked = false;
    }

    public String getApplicationId() { return applicationId; }
    public String getApplicantCitizenId() { return applicantCitizenId; }
    public String getProgramId() { return programId; }
    public String getStatus() { return status; }
    public LocalDateTime getDecisionDate() { return decisionDate; }
    public String getRejectionReason() { return rejectionReason; }
    public boolean isLocked() { return isLocked; }

    public void setStatus(String status) { this.status = status; }
    public void setDecisionDate(LocalDateTime decisionDate) { this.decisionDate = decisionDate; }
    public void setRejectionReason(String reason) { this.rejectionReason = reason; }
    
    // SR-4 & TS-8: Lock application during appeal review
    public void setLocked(boolean locked) { this.isLocked = locked; }
}