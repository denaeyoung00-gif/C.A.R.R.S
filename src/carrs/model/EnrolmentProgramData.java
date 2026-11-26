package carrs.model;

public class EnrolmentProgramData {
    private String programName;
    private int programID;
    private int maxCapacity;
    private int currentCount;
    private String startdate;
    private String enddate;

    public EnrolmentProgramData(String programName, int programID, int maxCapacity, int currentCount, String programDescription, String startdate, String enddate)
    {
        this.programName = programName;
        this.programID = programID;
        this.maxCapacity = maxCapacity;
        this.currentCount = 0;
        this.startdate = startdate;
        this.enddate = enddate;
    }

    public boolean hasCapacity(){
        return currentCount < maxCapacity;
    }

    public void addToProgram(){
        if (hasCapacity()){
            currentCount ++;
        } else {
            System.out.println ("The Program Is At Maximum Capacity");
        }
    }

    public void deleteFromProgram(){
        if (currentCount > 0){
            currentCount --;
        }
    }

    public String getProgramName()
    {
        return programName;
    }

    public int getProgramId(){
        return programID;
    }

    public int getMaxCapacity(){
        return maxCapacity;
    }
}
