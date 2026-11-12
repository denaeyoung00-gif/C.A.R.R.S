package carrs.model;

public enum Role {
    OFFICE_ATTENDANT(false), OFFICE_STAFF(true), OFFICE_MANAGER(true), MEMBER_OF_PARLIAMENT(true);
    private final boolean canTransfer;
    Role(boolean canTransfer) { this.canTransfer = canTransfer; }
    public boolean canTransfer() { return canTransfer; }
}
