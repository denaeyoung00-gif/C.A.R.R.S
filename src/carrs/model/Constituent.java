package carrs.model;

public class Constituent { private final String applicantID, firstName, lastName;
    private String email, phoneNumber, address, trn;
    private final char gender;
    private final String dOB;

    public Constituent(String firstName, String lastName, String email, String phoneNumber, String trn, String applicantID, String address, char gender, String dOB)
    {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.applicantID = applicantID;
        this.gender = gender;
        this.trn = trn;
        this.dOB = dOB;
    }

    public String getFirstName(){
        return firstName;
    }

    public String getLastName(){
        return lastName;
    }

    public String getEmail(){
        return email;
    }

    public String getPhoneNumber(){
        return phoneNumber;
    }

    public String getAddress(){
        return address;
    }

    public String getApplicantId(){
        return applicantID;
    }

    public char getGender(){
        return gender;
    }

    public String getTRN(){
        return trn;
    }

    public String getdOB(){
        return dOB;
    }

    public void setEmail(String email){
        this.email = email;
    }

    public void setPhoneNumber(String phoneNumber){
        this.phoneNumber = phoneNumber;
    }

    public void setAddress (String address){
        this.address = address;
    }
}

