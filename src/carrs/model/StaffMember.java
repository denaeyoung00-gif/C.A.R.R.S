package carrs.model;


public class StaffMember {
    private final int id;
    private final String name;
    private final Role role;

    public StaffMember(int id, String name, Role role) {
        this.id = id;
        this.name = name;
        this.role = role;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Role getRole() {
        return role;
    }

    /** Office Staff, Office Manager and MP may record and correct payments (not the Office Attendant). */
    public boolean canRecordPayments() { return role != Role.OFFICE_ATTENDANT; }

    /** Only the Office Manager and the MP may verify a payment. */
    public boolean canVerifyPayments() {
        return role == Role.OFFICE_MANAGER || role == Role.MEMBER_OF_PARLIAMENT;
    }
}
