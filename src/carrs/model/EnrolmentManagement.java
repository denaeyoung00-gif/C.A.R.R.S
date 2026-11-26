package carrs.model;
import java.util.ArrayList;
import java.util.List;
import java.lang.String;
import java.time.LocalDate;

public class EnrolmentManagement {
    private int nextEnrolId;
    private int nextID;
    private List<ProgramEnrolment> enrolments = new ArrayList<>();
    private List<Constituent> constituents = new ArrayList<>();
    private EnrolmentProgramData program;

    public EnrolmentManagement(){
        this(1,1);
    }

    public EnrolmentManagement(int nextEnrolId, int nextID){
        this.nextEnrolId = nextEnrolId;
        this.nextID = nextID;
    }

    public List<ProgramEnrolment> getEnrolments(){
        return enrolments;
    }

    public List<Constituent> getConstituents(){
        return constituents;
    }

    public Constituent enrollConstituent(String firstName, String lastName, String email, String trn, String phoneNumber, String address, char gender, String dOB){
        String applicantID = "PAR" + nextID++;
        Constituent newConstituent = new Constituent(firstName, lastName, email, trn, phoneNumber, applicantID, address, gender, dOB);

        constituents.add(newConstituent);
        return newConstituent;
    }

    public ProgramEnrolment enrollToProgram(Constituent constituent, EnrolmentProgramData program) {
        for (ProgramEnrolment e : enrolments){
            if (e.getConstituent().getApplicantId().equals(constituent.getApplicantId()) &&
                    e.getProgram().getProgramId() == (program.getProgramId())&& e.status() == EnrolmentStatus.ACTIVE){
                System.out.println("Constituent Is Already Enrolled In This Program");
                return null;
            }
        }

        if(program.hasCapacity()){
            String enrolmentId = "ENR" + nextEnrolId++;
            ProgramEnrolment newEnrolment = new ProgramEnrolment(enrolmentId, constituent, program, EnrolmentStatus.ACTIVE, LocalDate.now());

            program.addToProgram();
            newEnrolment.updateStatus(EnrolmentStatus.ACTIVE);
            enrolments.add(newEnrolment);

            return newEnrolment;
        } else if (!program.hasCapacity()){
            System.out.println("Program Is Full. Cannot Accept Any Additional Constituent");

        }
        return null;
    }

    public boolean editConstituentRecord(String applicantID, String email, String phoneNumber, String address){
        for(Constituent m : constituents){
            if (m.getApplicantId().equals(applicantID)){
                if(email != null) {
                    m.setEmail(email);
                }

                if (phoneNumber != null){
                    m.setPhoneNumber(phoneNumber);
                }

                if (address != null){
                    m.setAddress(address);
                }
                return true;
            }

        }
        return false;
    }

    public Constituent searchByFirstName(String firstName){
        for(Constituent p : constituents){
            if (p.getFirstName().equals(firstName)){
                return p;
            }
        }
        return null;
    }

    public Constituent searchByLastName(String lastName){
        for(Constituent l : constituents){
            if (l.getLastName().equals(lastName)){
                return l;
            }
        }
        return null;
    }

    public ProgramEnrolment searchEnrolmentByID(String applicantID){
        for(ProgramEnrolment g : enrolments){
            if(g.getEnrolmentId().equalsIgnoreCase(applicantID)){
                return g;
            }
        }
        return null;
    }

    public List<ProgramEnrolment> getEnrolmentsConstituentId(String applicantId){
        List<ProgramEnrolment> matches = new ArrayList<>();
        for (ProgramEnrolment x: enrolments){
            if(x.getConstituent().getApplicantId().equalsIgnoreCase(applicantId)) {
                matches.add(x);
            }
        }
        return matches;
    }
}
