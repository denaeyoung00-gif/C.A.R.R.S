import java.awt.*;
import java.time.LocalDate;
import javax.swing.*;

public class ProgrammeCard extends JPanel {

    public ProgrammeCard(Program p) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        setBackground(Color.WHITE);

        JLabel name = new JLabel(p.getProgramName());
        name.setFont(name.getFont().deriveFont(Font.BOLD, 16f));

        JLabel dates = new JLabel(p.getStartDate() + "  →  " + p.getFinishDate());
        dates.setForeground(Color.GRAY);

        JLabel status = new JLabel(p.computeStatus(LocalDate.now()).toString());
        status.setForeground(new Color(0, 120, 0));
        status.setFont(status.getFont().deriveFont(Font.BOLD));

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.add(name);
        center.add(Box.createVerticalStrut(4));
        center.add(dates);

        add(center, BorderLayout.CENTER);
        add(status, BorderLayout.EAST);
    }
}