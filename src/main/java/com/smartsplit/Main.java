package com.smartsplit;

import com.smartsplit.database.DBConnection;
import com.smartsplit.exception.DatabaseException;
import com.smartsplit.ui.AppContext;
import com.smartsplit.ui.LoginFrame;
import com.smartsplit.ui.UITheme;
import com.smartsplit.util.EnvConfig;

import java.awt.GraphicsEnvironment;

/** Application entry point. */
public final class Main {

    public static final String APP_NAME = "SmartSplit";
    public static final String TAGLINE  = "Split expenses. Settle smartly. Explore nearby.";

    private Main() {}

    public static void main(String[] args) {
        System.out.println(APP_NAME + " — " + TAGLINE);

        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("No display detected; skipping GUI startup.");
            return;
        }

        UITheme.applyLookAndFeel();

        AppContext ctx;
        try {
            EnvConfig   config = EnvConfig.load();
            DBConnection db    = DBConnection.fromConfig(config);
            ctx = new AppContext(db);
        } catch (DatabaseException e) {
            // Show a friendly error before any Swing frame is open
            System.err.println("Startup error: " + e.getMessage());
            javax.swing.JOptionPane.showMessageDialog(null,
                    e.getMessage(),
                    "SmartSplit — Configuration Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            return;
        }

        LoginFrame.show(ctx);
    }
}
