package carrs.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** One lodged appeal as stored in the database. */
public class AppealRecord {
    public static class Doc {
        public final String fileName; public final String extension; public final long sizeBytes;
        public Doc(String fileName, String extension, long sizeBytes) {
            this.fileName = fileName; this.extension = extension; this.sizeBytes = sizeBytes;
        }
    }

    private final String cadn;
    private final int applicationId;
    private final String constituencyCode;
    private final String grounds;          // enum names separated by commas
    private final String justification;
    private String status;                 // APPEAL_PENDING_REVIEW / APPEAL_APPROVED / APPEAL_DENIED
    private final String lodgedBy;
    private final LocalDateTime lodgedAt;
    private String decidedBy;
    private String decisionNote;
    private LocalDateTime decidedAt;
    private final List<Doc> documents = new ArrayList<>();

    public AppealRecord(String cadn, int applicationId, String constituencyCode, String grounds,
                        String justification, String status, String lodgedBy, LocalDateTime lodgedAt,
                        String decidedBy, String decisionNote, LocalDateTime decidedAt) {
        this.cadn = cadn; this.applicationId = applicationId; this.constituencyCode = constituencyCode;
        this.grounds = grounds; this.justification = justification; this.status = status;
        this.lodgedBy = lodgedBy; this.lodgedAt = lodgedAt;
        this.decidedBy = decidedBy; this.decisionNote = decisionNote; this.decidedAt = decidedAt;
    }

    public String getCadn() { return cadn; }
    public int getApplicationId() { return applicationId; }
    public String getConstituencyCode() { return constituencyCode; }
    public String getGrounds() { return grounds; }
    public String getJustification() { return justification; }
    public String getStatus() { return status; }
    public String getLodgedBy() { return lodgedBy; }
    public LocalDateTime getLodgedAt() { return lodgedAt; }
    public String getDecidedBy() { return decidedBy; }
    public String getDecisionNote() { return decisionNote; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
    public List<Doc> getDocuments() { return documents; }
}
