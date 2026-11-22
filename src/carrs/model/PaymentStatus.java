package carrs.model;

public enum PaymentStatus {
    PENDING, PROCESSED, COMPLETED, FAILED;

    /** Allowed order: Pending -> Processed -> Completed. Pending or Processed may fail. */
    public boolean canTransitionTo(PaymentStatus next) {
        switch (this) {
            case PENDING:   return next == PROCESSED || next == FAILED;
            case PROCESSED: return next == COMPLETED || next == FAILED;
            default:        return false;   // COMPLETED and FAILED are final
        }
    }
}
