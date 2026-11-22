package carrs.model;

/**
 * How a payment reaches the constituent. The system never pays anyone directly: the office prints a cheque,
 * hands it over, then scans it and attaches the scan. So CHEQUE is the only method.
 */
public enum PaymentMethod {
    CHEQUE("Cheque");

    private final String label;
    PaymentMethod(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
