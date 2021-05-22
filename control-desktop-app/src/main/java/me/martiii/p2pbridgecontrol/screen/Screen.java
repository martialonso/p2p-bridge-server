package me.martiii.p2pbridgecontrol.screen;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

public class Screen extends JFrame {
    private final ConnectionPanel con1;
    private final ConnectionPanel con2;

    private final JTextArea dataArea;
    private final JCheckBox asCheckBox;

    public Screen() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 400);

        setLayout(new BorderLayout());
        setTitle("P2PServerBridge Control Desktop App");

        con1 = new ConnectionPanel(1);
        con2 = new ConnectionPanel(2);

        add(con1,  BorderLayout.WEST);
        add(con2, BorderLayout.EAST);

        JPanel vbox = new JPanel();
        BoxLayout vboxLayout = new BoxLayout(vbox, BoxLayout.Y_AXIS);
        vbox.setLayout(vboxLayout);

        dataArea = new JTextArea();
        dataArea.setDisabledTextColor(Color.BLACK);
        dataArea.setEnabled(false);
        JScrollPane scrollPane = new JScrollPane(dataArea);
        scrollPane.setAlignmentX(Component.RIGHT_ALIGNMENT);

        asCheckBox = new JCheckBox("Autoscroll", true);
        asCheckBox.setFocusPainted(false);
        asCheckBox.setAlignmentX(Component.RIGHT_ALIGNMENT);
        asCheckBox.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                dataArea.setCaretPosition(dataArea.getDocument().getLength());
            }
        });

        vbox.add(scrollPane);
        vbox.add(asCheckBox);
        add(vbox);

        setVisible(true);
    }

    public void log(String msg) {
        dataArea.append(msg + "\n");
        if (asCheckBox.isSelected()) {
            dataArea.setCaretPosition(dataArea.getDocument().getLength());
        }
        if (msg.startsWith("connected")) { //connected1 127.0.0.1:00001
            connectionUpdate(msg.charAt(9), msg.substring(11));
        } else if (msg.startsWith("disconnected")) { //disconnected1 127.0.0.1:00001
            connectionUpdate(msg.charAt(12), null);
        }
    }

    public void connectionUpdate(char id, String address) {
        if (id != '1' && id != '2') return;
        ConnectionPanel connectionPanel = id == '1' ? con1 : con2;
        connectionPanel.connectionUpdate(address);
    }
}
