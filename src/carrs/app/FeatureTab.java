package carrs.app;

import carrs.model.StaffMember;
import javax.swing.JComponent;

/**
 * One tab in the C.A.R.R.S window. Every feature (Register Applicant, Programme Management, Payments...)
 * is a class that implements this interface, so the window can show it without knowing what is inside.
 *
 * TO ADD YOUR FEATURE: write a class that implements FeatureTab (see PaymentTab as an example),
 * then add ONE line to CarrsApp.registerTabs().
 */
public interface FeatureTab {
    /** The text on the tab, e.g. "Register Applicant". */
    String getTitle();

    /** The screen shown when the tab is clicked (usually a JPanel). */
    JComponent getView();

    /** Called whenever someone logs in as a different staff member, so the tab can show or hide things by role. */
    void onUserChanged(StaffMember user);
}
