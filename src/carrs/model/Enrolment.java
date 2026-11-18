// Enrolment.java
package carrs.model;
import java.time.LocalDateTime;

public class    Enrolment {
    private int id;
    private final int applicantId, programmeId;
    private EnrolmentStatus status;
    private final LocalDateTime enrolledAt;
    private final Integer transferredFromEnrolmentId;   // set only when created by a transfer

    public Enrolment(int id, int applicantId, int programmeId, EnrolmentStatus status,
                     LocalDateTime enrolledAt, Integer transferredFromEnrolmentId) {
        this.id = id; this.applicantId = applicantId; this.programmeId = programmeId;
        this.status = status; this.enrolledAt = enrolledAt;
        this.transferredFromEnrolmentId = transferredFromEnrolmentId;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getApplicantId() { return applicantId; }
    public int getProgrammeId() { return programmeId; }
    public EnrolmentStatus getStatus() { return status; }
    public void setStatus(EnrolmentStatus status) { this.status = status; }
    public LocalDateTime getEnrolledAt() { return enrolledAt; }
    public Integer getTransferredFromEnrolmentId() { return transferredFromEnrolmentId; }
    public boolean isLive() { return status == EnrolmentStatus.ACTIVE || status == EnrolmentStatus.ON_HOLD; }
    public Enrolment copy() {
        return new Enrolment(id, applicantId, programmeId, status, enrolledAt, transferredFromEnrolmentId);
    }
}