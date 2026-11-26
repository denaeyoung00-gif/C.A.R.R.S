package carrs.model;

import java.time.LocalDate;

/** One line of the Enrolment Management screens: an enrolment joined to its applicant and programme. */
public class EnrolmentRow {
    private final int enrolmentId, applicantId, programmeId;
    private final String applicantName, trn, programmeName, address, phone;
    private final EnrolmentStatus status;
    private final LocalDate enrolledOn;

    public EnrolmentRow(int enrolmentId, int applicantId, int programmeId, String applicantName, String trn,
                        String programmeName, EnrolmentStatus status, String address, String phone,
                        LocalDate enrolledOn) {
        this.enrolmentId = enrolmentId; this.applicantId = applicantId; this.programmeId = programmeId;
        this.applicantName = applicantName; this.trn = trn; this.programmeName = programmeName;
        this.status = status; this.address = address; this.phone = phone; this.enrolledOn = enrolledOn;
    }
    public int getEnrolmentId() { return enrolmentId; }
    public int getApplicantId() { return applicantId; }
    public int getProgrammeId() { return programmeId; }
    public String getApplicantName() { return applicantName; }
    public String getTrn() { return trn == null ? "" : trn; }
    public String getProgrammeName() { return programmeName; }
    public EnrolmentStatus getStatus() { return status; }
    public String getAddress() { return address == null ? "" : address; }
    public String getPhone() { return phone == null ? "" : phone; }
    public LocalDate getEnrolledOn() { return enrolledOn; }
}