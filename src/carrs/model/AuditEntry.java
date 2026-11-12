package carrs.model;

import java.time.LocalDateTime;

public class AuditEntry {
    private final int staffId, sourceProgrammeId, destinationProgrammeId, applicantId;
    private final LocalDateTime timestamp;
    private final String reason;

    public AuditEntry(int staffId, LocalDateTime timestamp, int sourceProgrammeId,
                      int destinationProgrammeId, String reason, int applicantId) {
        this.staffId = staffId; this.timestamp = timestamp;
        this.sourceProgrammeId = sourceProgrammeId; this.destinationProgrammeId = destinationProgrammeId;
        this.reason = reason; this.applicantId = applicantId;
    }

    public int getStaffId() { return staffId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public int getSourceProgrammeId() { return sourceProgrammeId; }
    public int getDestinationProgrammeId() { return destinationProgrammeId; }
    public String getReason() { return reason; }
    public int getApplicantId() { return applicantId; }
}