package carrs.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Payment {
    private int id;                          // 0 until saved
    private final int applicantId;
    private final int programmeId;
    private BenefitType benefitType;
    private BigDecimal amount;               // BigDecimal, not double: it is money
    private LocalDate scheduledDate;
    private LocalDate distributedDate;       // null until completed
    private PaymentStatus status;
    private String failureReason;            // null unless failed
    private String payeeName;                // who the money goes to
    private boolean verified;
    private Integer verifiedBy;              // staff id, null until verified
    private LocalDateTime verifiedAt;
    private final int recordedBy;
    private final LocalDateTime recordedAt;

    public Payment(int id, int applicantId, int programmeId, BenefitType benefitType,
                   BigDecimal amount, LocalDate scheduledDate, LocalDate distributedDate,
                   PaymentStatus status, String failureReason, String payeeName,
                   boolean verified, Integer verifiedBy, LocalDateTime verifiedAt,
                   int recordedBy, LocalDateTime recordedAt) {
        this.id = id; this.applicantId = applicantId; this.programmeId = programmeId;
        this.benefitType = benefitType; this.amount = amount;
        this.scheduledDate = scheduledDate; this.distributedDate = distributedDate;
        this.status = status; this.failureReason = failureReason; this.payeeName = payeeName;
        this.verified = verified; this.verifiedBy = verifiedBy; this.verifiedAt = verifiedAt;
        this.recordedBy = recordedBy; this.recordedAt = recordedAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getApplicantId() { return applicantId; }
    public int getProgrammeId() { return programmeId; }
    public BenefitType getBenefitType() { return benefitType; }
    public void setBenefitType(BenefitType benefitType) { this.benefitType = benefitType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDate scheduledDate) { this.scheduledDate = scheduledDate; }
    public LocalDate getDistributedDate() { return distributedDate; }
    public void setDistributedDate(LocalDate distributedDate) { this.distributedDate = distributedDate; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getPayeeName() { return payeeName; }
    public void setPayeeName(String payeeName) { this.payeeName = payeeName; }
    public Integer getVerifiedBy() { return verifiedBy; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public int getRecordedBy() { return recordedBy; }
    public LocalDateTime getRecordedAt() { return recordedAt; }

    public boolean isVerified() { return verified; }
    public void markVerified(int staffId, LocalDateTime when) {
        this.verified = true; this.verifiedBy = staffId; this.verifiedAt = when;
    }
    public void clearVerification() {
        this.verified = false; this.verifiedBy = null; this.verifiedAt = null;
    }

    /** Money has actually been given out. */
    public boolean isDistributed() { return status == PaymentStatus.COMPLETED; }

    public Payment copy() {
        return new Payment(id, applicantId, programmeId, benefitType, amount, scheduledDate,
                distributedDate, status, failureReason, payeeName, verified, verifiedBy,
                verifiedAt, recordedBy, recordedAt);
    }
}
