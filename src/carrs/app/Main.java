package carrs.app;

import carrs.model.*;
import carrs.repository.*;
import carrs.service.*;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        TransferService service = new TransferService(new MySqlCarrsRepository());
        StaffMember staff = new StaffMember(1, "Alexia James", Role.OFFICE_MANAGER);
        Scanner in = new Scanner(System.in);
        try {
            System.out.print("Enrolment ID to transfer: ");
            int enrolmentId = Integer.parseInt(in.nextLine().trim());

            List<Programme> options = service.getEligibleDestinations(staff, enrolmentId);
            System.out.println("Programmes you can move to:");
            for (Programme p : options) System.out.println("  " + p.getId() + " - " + p.getName());

            System.out.print("Destination programme ID: ");
            int destId = Integer.parseInt(in.nextLine().trim());
            System.out.print("Reason (min 10 characters): ");
            String reason = in.nextLine();

            Enrolment created = service.transfer(staff, enrolmentId, destId, reason);
            System.out.println("Transfer complete. New enrolment ID: " + created.getId());
        } catch (TransferException e) {
            System.out.println("Transfer blocked: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Please enter numbers for the IDs.");
        }
    }
}