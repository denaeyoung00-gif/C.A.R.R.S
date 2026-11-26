import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

enum AppealStatus {
    APPEAL_PENDING_REVIEW,
    UNDER_INVESTIGATION,
    APPEAL_APPROVED,
    APPEAL_DENIED,
}

enum AppealGround {
    INCORRECT_INCOME_ASSESSMENT,
    OVERLOOKED_DOCUMENTATION,
    UPDATED_HOUSEHOLD_DEPENDENCY,
    ADMINISTRATIVE_ERROR
}

public class Appeal {
    private final String cadn;
    private final String originalApplicationId;
    private final String applicantCitizenId;
    private final String constituencyCode;
    private final List<AppealGround> grounds;
    private final String justification;
    private final List<SupportingDocument> supportingDocs;
    private final LocalDateTime submissionTimestamp;
    private AppealStatus status;
    private final List<String> reviewAuditTrail = new ArrayList<>();
    private String decidedBy;
    private String decisionNote;
    private LocalDateTime decisionTimestamp;
    private LocalDateTime archivedUntil;
    private boolean finalized;

    public Appeal(String cadn,
            String originalApplicationId,
            String applicantCitizenId,
            String constituencyCode,
            List<AppealGround> grounds,
            String justification,
            List<SupportingDocument> supportingDocs) {
        this.cadn = cadn;
        this.originalApplicationId = originalApplicationId;
        this.applicantCitizenId = applicantCitizenId;
        this.constituencyCode = constituencyCode;
        this.grounds = new ArrayList<>(grounds);
        this.justification = justification;
        this.supportingDocs = new ArrayList<>(supportingDocs);
        this.submissionTimestamp = LocalDateTime.now();
        this.status = AppealStatus.APPEAL_PENDING_REVIEW;
        this.finalized = false;
    }

    public String getCadn() {
        return cadn;
    }

    public String getOriginalApplicationId() {
        return originalApplicationId;
    }

    public String getApplicantCitizenId() {
        return applicantCitizenId;
    }

    public String getConstituencyCode() {
        return constituencyCode;
    }

    public List<AppealGround> getGrounds() {
        return new ArrayList<>(grounds);
    }

    public String getJustification() {
        return justification;
    }

    public List<SupportingDocument> getSupportingDocs() {
        return new ArrayList<>(supportingDocs);
    }

    public LocalDateTime getSubmissionTimestamp() {
        return submissionTimestamp;
    }

    public AppealStatus getStatus() {
        return status;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public String getDecisionNote() {
        return decisionNote;
    }

    public LocalDateTime getDecisionTimestamp() {
        return decisionTimestamp;
    }

    public LocalDateTime getArchivedUntil() {
        return archivedUntil;
    }

    public boolean isFinalized() {
        return finalized;
    }

    public List<String> getReviewAuditTrail() {
        return new ArrayList<>(reviewAuditTrail);
    }

    public void recordReviewSession(String reviewerId) {
        reviewAuditTrail.add(LocalDateTime.now() + " | Reviewer " + reviewerId + " opened docket " + cadn);
    }

    public void finalizeDecision(AppealStatus outcomeStatus, String reviewerId, String decisionNote,
            LocalDateTime archivedUntil) {
        if (this.finalized) {
            throw new IllegalStateException("Finalized Error: Appeal " + cadn
                    + " has already been decided and cannot be edited or re-adjudicated.");
        }
        this.status = outcomeStatus;
        this.decidedBy = reviewerId;
        this.decisionNote = decisionNote;
        this.decisionTimestamp = LocalDateTime.now();
        this.archivedUntil = archivedUntil;
        this.finalized = true;
    }

    public void updateStatus(AppealStatus newStatus) {
        if (this.finalized) {
            throw new IllegalStateException(
                    "Finalized Error: Appeal " + cadn + " has already been finalized and cannot be modified.");
        }
        this.status = newStatus;
    }

    // AC-4: Generate a printable tracking receipt with CADN and timestamp
    public String generateReceipt() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return "==========================================================\n" +
                "        CONSTITUENCY APPEAL TRACKING RECEIPT (AC-4)       \n" +
                "==========================================================\n" +
                "Docket Number (CADN) : " + cadn + "\n" +
                "Original App ID      : " + originalApplicationId + "\n" +
                "Applicant Citizen ID : " + applicantCitizenId + "\n" +
                "Constituency Code    : " + constituencyCode + "\n" +
                "Current Status       : " + status + "\n" +
                "Audit Timestamp      : " + submissionTimestamp.format(dtf) + "\n" +
                "Grounds Selected     : " + grounds + "\n" +
                "Documents Attached   : " + supportingDocs.size() + " file(s)\n" +
                "==========================================================";
    }
}