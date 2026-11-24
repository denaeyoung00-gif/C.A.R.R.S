package carrs.model;

import java.time.LocalDateTime;

/** One recorded change to a payment: what changed, the old and new value, why, who and when. */
public class PaymentChange {
    private int id;
    private final int paymentId;
    private final String fieldName;
    private final String oldValue;
    private final String newValue;
    private final String reason;
    private final int changedBy;
    private final LocalDateTime changedAt;

    public PaymentChange(int id, int paymentId, String fieldName, String oldValue,
                         String newValue, String reason, int changedBy, LocalDateTime changedAt) {
        this.id = id; this.paymentId = paymentId; this.fieldName = fieldName;
        this.oldValue = oldValue; this.newValue = newValue; this.reason = reason;
        this.changedBy = changedBy; this.changedAt = changedAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getPaymentId() { return paymentId; }
    public String getFieldName() { return fieldName; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
    public String getReason() { return reason; }
    public int getChangedBy() { return changedBy; }
    public LocalDateTime getChangedAt() { return changedAt; }
}
