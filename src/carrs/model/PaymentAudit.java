package carrs.model;

import java.time.LocalDateTime;

/** One line of the payment audit trail: who did what, to which payment, and when. */
public class PaymentAudit {
    private int id;
    private Integer paymentId;          // null for actions not tied to one payment (e.g. viewing a history)
    private final String action;        // RECORDED, STATUS_CHANGED, CORRECTED, VERIFIED, CHEQUE_PRINTED, ...
    private final String details;
    private final int performedBy;
    private final LocalDateTime performedAt;

    public PaymentAudit(int id, Integer paymentId, String action, String details,
                        int performedBy, LocalDateTime performedAt) {
        this.id = id; this.paymentId = paymentId; this.action = action; this.details = details;
        this.performedBy = performedBy; this.performedAt = performedAt;
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Integer getPaymentId() { return paymentId; }
    public void setPaymentId(Integer paymentId) { this.paymentId = paymentId; }
    public String getAction() { return action; }
    public String getDetails() { return details; }
    public int getPerformedBy() { return performedBy; }
    public LocalDateTime getPerformedAt() { return performedAt; }
}
