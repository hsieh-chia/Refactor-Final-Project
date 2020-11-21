/*
 * Copyright 2020 Laszlo Balazs-Csiki and Contributors
 *
 * This file is part of Pixelitor. Pixelitor is free software: you
 * can redistribute it and/or modify it under the terms of the GNU
 * General Public License, version 3 as published by the Free
 * Software Foundation.
 *
 * Pixelitor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Pixelitor. If not, see <http://www.gnu.org/licenses/>.
 */

package pixelitor.menus.help;

import pixelitor.Pixelitor;
import pixelitor.gui.utils.DialogBuilder;
import pixelitor.utils.OpenInBrowserAction;

import javax.swing.*;
import java.awt.Dimension;
import java.net.URL;

import static java.awt.Component.CENTER_ALIGNMENT;
import static javax.swing.SwingConstants.CENTER;

/**
 * The "About" dialog of the app.
 */
public class AboutDialog {
    private static Box box;
    public static final String HOME_PAGE = "https://pixelitor.sourceforge.io";

    private AboutDialog() {
        // should not be instantiated
    }

    public static void showDialog(String aboutText) {
        createAboutBox();

        var tabbedPane = new JTabbedPane();

        tabbedPane.add("About", box);
        tabbedPane.add("Credits", createCreditsPanel());
        tabbedPane.add("System Info", new SystemInfoPanel());

        new DialogBuilder()
            .title(aboutText)
            .content(tabbedPane)
            .withScrollbars()
            .noCancelButton()
            .show();
    }

    private static JPanel createCreditsPanel() {
        var p = new JPanel();
        String text = "<html>Pixelitor was written by <b>L\u00e1szl\u00f3 Bal\u00e1zs-Cs\u00edki</b>." +
            "<br><br><b>Łukasz Kurzaj</b> contributed many improvements," +
            "<br>see the release notes</b>." +
            "<br>The Sepia filter was contributed by <b>Daniel Wreczycki</b>." +
            "<br><br>Pixelitor uses <ul><li>the image filter library by <b>Jerry Huxtable</b> " +
            "<li>many components by <b>Jeremy Wood</b>" +
            "<li>the fast math library by <b>Jeff Hain</b>" +
            "<li>the metadata library by <b>Drew Noakes</b>" +
            "<li>the animated GIF encoder by <b>Kevin Weiner</b>" +
            "<li>the GIF decoder by <b>Dhyan Blum</b>" +
            "<li>the Canny Edge Detector by <b>Tom Gibara</b>" +
            "<li>the SwingX library";
        p.add(new JLabel(text));

        return p;
    }

    private static void createAboutBox() {
        box = Box.createVerticalBox();

        addLabel(AboutDialog.class.getResource("/images/pixelitor_icon48.png"));

        addLabel("<html><b><font size=+1>Pixelitor</font></b></html>");
        addLabel("Version " + Pixelitor.VERSION_NUMBER);
        box.add(Box.createRigidArea(new Dimension(10, 20)));
        addLabel("<html><center> Copyright \u00A9 2009-2020 L\u00E1szl\u00F3 Bal\u00E1zs-Cs\u00EDki " +
            "<br>and Contributors<br><br>");
        addLabel("lbalazscs\u0040gmail.com");

        box.add(createLinkButton());
        box.add(Box.createGlue());
    }

    private static JButton createLinkButton() {
        var linkButton = new JButton("<HTML><FONT color=\"#000099\"><U>" + HOME_PAGE + "</U></FONT></HTML>");

        linkButton.setHorizontalAlignment(CENTER);
        linkButton.setBorderPainted(false);
        linkButton.setFocusPainted(false);
        linkButton.setOpaque(false);
        linkButton.setBackground(box.getBackground());
        linkButton.setAlignmentX(CENTER_ALIGNMENT);

        linkButton.addActionListener(new OpenInBrowserAction(null, HOME_PAGE));
        return linkButton;
    }

    private static void addLabel(URL url) {
        var imageIcon = new ImageIcon(url);
        var label = new JLabel(imageIcon, CENTER);
        label.setAlignmentX(CENTER_ALIGNMENT);
        box.add(label);
    }

    private static void addLabel(String text) {
        var label = new JLabel(text, CENTER);
        label.setAlignmentX(CENTER_ALIGNMENT);
        box.add(label);
    }
}


