package carrs.service;

/** A payment action was blocked; the message says why, and nothing was changed. */
public class PaymentException extends Exception {
    public PaymentException(String message) { super(message); }
}
