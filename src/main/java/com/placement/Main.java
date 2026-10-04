package com.placement;

import com.placement.database.DatabaseConnection;
import com.placement.ui.LoginFrame;

import javax.swing.*;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        try {
            DatabaseConnection.verifyConnection();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Database connection failed.\n\n" + e.getMessage() +
                    "\n\nCheck MySQL and DB_URL / DB_USER / DB_PASSWORD.",
                    "Placement Portal",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
