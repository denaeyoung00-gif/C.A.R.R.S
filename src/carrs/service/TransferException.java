package carrs.service;

public class TransferException extends Exception {
    public TransferException(String message) { super(message); }
    public TransferException(String message, Throwable cause) { super(message, cause); }
}
