package me.martiii.p2pbridgecontrol.screen;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;

public class Screen extends JFrame {
    private ConnectionPanel con1;
    private ConnectionPanel con2;

    private JTextArea dataArea;

    public Screen() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 400);

        setLayout(new BorderLayout());
        setTitle("P2PServerBridge Control Desktop App");

        con1 = new ConnectionPanel(1);
        con2 = new ConnectionPanel(2);

        add(con1,  BorderLayout.WEST);
        add(con2, BorderLayout.EAST);

        dataArea = new JTextArea();
        dataArea.setDisabledTextColor(Color.BLACK);
        dataArea.setEnabled(false);
        DefaultCaret caret = (DefaultCaret) dataArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        JScrollPane scrollPane = new JScrollPane(dataArea);
        add(scrollPane);

        setVisible(true);
    }

    public void log(String msg) {
        dataArea.append(msg + "\n");
        if (msg.startsWith("connected")) { //connected1 127.0.0.1:00001
            if (msg.charAt(9) == '1') {
                con1.connectionUpdate(msg.substring(11));
            } else {
                con2.connectionUpdate(msg.substring(11));
            }
        } else if (msg.startsWith("disconnected")) { //disconnected1 127.0.0.1:00001
            if (msg.charAt(12) == '1') {
                con1.connectionUpdate(null);
            } else {
                con2.connectionUpdate(null);
            }
        }
    }
}
