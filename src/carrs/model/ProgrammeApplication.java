package carrs.model;

import java.time.LocalDateTime;

public class ProgrammeApplication {
    private int id;
    private final String referenceNo;
    private final int applicantId;
    private final int programmeId;
    private final String assistanceRequested;
    private final String reasonForApplying;
    private ApplicationStatus status;
    private String rejectionReason;
    private Integer decidedBy;
    private LocalDateTime decidedAt;
    private final int createdBy;
    private final LocalDateTime createdAt;
    private final String supportingDocPath;

    public ProgrammeApplication(int id, String referenceNo, int applicantId, int programmeId,
                                String assistanceRequested, String reasonForApplying,
                                ApplicationStatus status, String rejectionReason, Integer decidedBy,
                                LocalDateTime decidedAt, int createdBy, LocalDateTime createdAt,
                                String supportingDocPath) {
        this.id = id; this.referenceNo = referenceNo; this.applicantId = applicantId;
        this.programmeId = programmeId; this.assistanceRequested = assistanceRequested;
        this.reasonForApplying = reasonForApplying; this.status = status;
        this.rejectionReason = rejectionReason; this.decidedBy = decidedBy;
        this.decidedAt = decidedAt; this.createdBy = createdBy;
        this.createdAt = createdAt; this.supportingDocPath = supportingDocPath;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getReferenceNo() { return referenceNo; }
    public int getApplicantId() { return applicantId; }
    public int getProgrammeId() { return programmeId; }
    public String getAssistanceRequested() { return assistanceRequested; }
    public String getReasonForApplying() { return reasonForApplying; }
    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String reason) { this.rejectionReason = reason; }
    public Integer getDecidedBy() { return decidedBy; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
    public int getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getSupportingDocPath() { return supportingDocPath; }
}
