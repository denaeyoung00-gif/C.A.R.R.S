package carrs.model;

import java.time.LocalDate;

public class ProgramEnrolment {
    private String enrolmentId;
    private Constituent constituent;
    private EnrolmentProgramData program;
    private EnrolmentStatus status;
    private LocalDate enrolmentDate;

    public ProgramEnrolment(String enrolmentId, Constituent constituent, EnrolmentProgramData program, EnrolmentStatus status, LocalDate enrolmentDate)
    {
        this.enrolmentId = enrolmentId;
        this.constituent = constituent;
        this.program = program;
        this.status = status != null ? status : EnrolmentStatus.ACTIVE;
        this.enrolmentDate = enrolmentDate != null ? enrolmentDate : LocalDate.now();
    }

    public void updateStatus(EnrolmentStatus status){
        this.status = status;
    }

    public String getEnrolmentId()
    {
        return enrolmentId;
    }

    public Constituent getConstituent(){
        return constituent;
    }

    public EnrolmentProgramData getProgram(){
        return program;
    }

    public EnrolmentStatus status(){
        return status;
    }

    public LocalDate getEnrolmentDate(){
        return enrolmentDate;
    }

    public void setEnrolmentDate(LocalDate enrolmentDate){
        this.enrolmentDate = enrolmentDate;
    }
}
