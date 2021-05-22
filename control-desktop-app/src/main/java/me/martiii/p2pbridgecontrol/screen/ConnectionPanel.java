package me.martiii.p2pbridgecontrol.screen;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public class ConnectionPanel extends JPanel {
    private static final Color RED = new Color(255, 100, 100);
    private static final Color GREEN = new Color(100, 255, 100);

    private JLabel statusLabel;
    private JLabel addressLabel;

    public ConnectionPanel(int id) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel nameLabel = new JLabel("Connection " + id);
        nameLabel.setFont(nameLabel.getFont().deriveFont(20f));
        add(nameLabel);

        add(Box.createRigidArea(new Dimension(0, 10)));

        addressLabel = new JLabel();
        add(addressLabel);

        add(Box.createRigidArea(new Dimension(0, 10)));

        statusLabel = new JLabel("Disconnected");
        statusLabel.setBorder(new CompoundBorder(new LineBorder(Color.BLACK), new EmptyBorder(8, 8, 8, 8)));
        statusLabel.setBackground(RED);
        statusLabel.setOpaque(true);
        add(statusLabel);
    }

    /* address != null -> connected, address == null -> disconnected */
    public void connectionUpdate(String address) {
        if (address != null) {
            statusLabel.setText("Connected");
            statusLabel.setBackground(GREEN);
            addressLabel.setText(address);
        } else {
            statusLabel.setText("Disconnected");
            statusLabel.setBackground(RED);
            addressLabel.setText("");
        }
    }
}
