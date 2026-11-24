package carrs.app;

import carrs.model.StaffMember;
import java.awt.*;
import javax.swing.*;

/** A stand-in tab for a feature whose code has not been plugged in yet. Replace it with the real tab. */
public class PlaceholderTab implements FeatureTab {
    private final String title;
    private final JPanel view = new JPanel(new GridBagLayout());

    public PlaceholderTab(String title, String owner, String featureId) {
        this.title = title;
        JLabel head = new JLabel(title, SwingConstants.CENTER);
        head.setFont(head.getFont().deriveFont(Font.BOLD, 22f));
        JLabel line = new JLabel("<html><div style='text-align:center'>" + featureId + " &mdash; owner: " + owner
                + "<br><br>This tab is waiting for its code.<br>"
                + "Implement <b>FeatureTab</b> and replace this placeholder in <b>CarrsApp.registerTabs()</b>.</div></html>",
                SwingConstants.CENTER);
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        head.setAlignmentX(Component.CENTER_ALIGNMENT);
        line.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(head); box.add(Box.createVerticalStrut(12)); box.add(line);
        view.add(box);
    }
    @Override public String getTitle() { return title; }
    @Override public JComponent getView() { return view; }
    @Override public void onUserChanged(StaffMember user) { }
}
