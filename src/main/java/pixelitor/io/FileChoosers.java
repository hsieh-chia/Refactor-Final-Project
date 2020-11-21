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

package pixelitor.io;

import pixelitor.Composition;
import pixelitor.gui.GlobalEvents;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.utils.Dialogs;
import pixelitor.gui.utils.ImagePreviewPanel;
import pixelitor.gui.utils.SaveFileChooser;
import pixelitor.utils.Messages;
import pixelitor.utils.ProgressPanel;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.io.File;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.SOUTH;
import static pixelitor.utils.Texts.i18n;
import static pixelitor.utils.Threads.calledOnEDT;
import static pixelitor.utils.Threads.threadInfo;

/**
 * Utility class with static methods related to file choosers
 */
public class FileChoosers {
    private static JFileChooser openChooser;
    private static SaveFileChooser saveChooser;

    public static final FileFilter jpegFilter = new FileNameExtensionFilter("JPEG files", "jpg", "jpeg");
    private static final FileFilter pngFilter = new FileNameExtensionFilter("PNG files", "png");
    private static final FileFilter bmpFilter = new FileNameExtensionFilter("BMP files", "bmp");
    public static final FileNameExtensionFilter gifFilter = new FileNameExtensionFilter("GIF files", "gif");
    private static final FileFilter tiffFilter = new FileNameExtensionFilter("TIFF files", "tiff", "tif");
    private static final FileFilter pxcFilter = new FileNameExtensionFilter("PXC files", "pxc");
    public static final FileFilter oraFilter = new FileNameExtensionFilter("OpenRaster files", "ora");

    private static final FileFilter[] OPEN_SAVE_FILTERS = {
        bmpFilter, gifFilter, jpegFilter, oraFilter,
        pngFilter, pxcFilter, tiffFilter};

    private FileChoosers() {
    }

    private static void initOpenChooser() {
        assert calledOnEDT() : threadInfo();

        if (openChooser == null) {
            createOpenChooser();
        }
    }

    private static void createOpenChooser() {
        openChooser = new JFileChooser(Dirs.getLastOpen()) {
            @Override
            public void approveSelection() {
                File f = getSelectedFile();
                if (!f.exists()) {
                    Dialogs.showErrorDialog("File not found",
                        "<html>The file <b>" + f.getAbsolutePath()
                            + " </b> does not exist. " +
                            "<br>Check the file name and try again."
                    );
                    return;
                }
                super.approveSelection();
            }
        };
        openChooser.setName("open");

        setDefaultOpenExtensions();

        var p = new JPanel();
        p.setLayout(new BorderLayout());
        var progressPanel = new ProgressPanel();
        var preview = new ImagePreviewPanel(progressPanel);
        p.add(preview, CENTER);
        p.add(progressPanel, SOUTH);

        openChooser.setAccessory(p);
        openChooser.addPropertyChangeListener(preview);
    }

    public static void initSaveChooser() {
        assert calledOnEDT() : threadInfo();

        File lastSaveDir = Dirs.getLastSave();
        if (saveChooser == null) {
            createSaveChooser(lastSaveDir);
        } else {
            saveChooser.setCurrentDirectory(lastSaveDir);
        }
    }

    private static void createSaveChooser(File lastSaveDir) {
        saveChooser = new SaveFileChooser(lastSaveDir);
        saveChooser.setName("save");
        saveChooser.setDialogTitle(i18n("save_as"));

        setDefaultSaveExtensions();
    }

    public static void openAsync() {
        initOpenChooser();

        GlobalEvents.dialogOpened("Open");
        int result = openChooser.showOpenDialog(PixelitorWindow.get());
        GlobalEvents.dialogClosed("Open");

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = openChooser.getSelectedFile();
            String fileName = selectedFile.getName();

            Dirs.setLastOpen(selectedFile.getParentFile());

            if (FileUtils.hasSupportedInputExt(fileName)) {
                IO.openFileAsync(selectedFile);
            } else { // unsupported extension
                handleUnsupportedExtensionWhileOpening(fileName);
            }
        } else if (result == JFileChooser.CANCEL_OPTION) {
            // cancelled
        } else if (result == JFileChooser.ERROR_OPTION) {
            // error or dismissed
        }
    }

    private static void handleUnsupportedExtensionWhileOpening(String fileName) {
        String extension = FileUtils.findExtension(fileName).orElse("");
        String msg = "Could not open " + fileName + ", because ";
        if (extension.isEmpty()) {
            msg += "it has no extension.";
        } else {
            msg += "files of type " + extension + " are not supported.";
        }
        Messages.showError("Error", msg);
    }

    public static boolean showSaveChooserAndSaveComp(Composition comp,
                                                     Object extraInfo) {
        String defaultFileName = FileUtils.stripExtension(comp.getName());
        saveChooser.setSelectedFile(new File(defaultFileName));

        File file = comp.getFile();
        if (file != null && Dirs.getLastSave() == null) {
            File customSaveDir = file.getParentFile();
            saveChooser.setCurrentDirectory(customSaveDir);
        }
        assert saveChooser.getFileSelectionMode() == JFileChooser.FILES_ONLY;

        GlobalEvents.dialogOpened("Save");
        int result = saveChooser.showSaveDialog(PixelitorWindow.get());
        GlobalEvents.dialogClosed("Save");

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = saveChooser.getSelectedFile();
            String extension = saveChooser.getExtension();
            IO.saveToChosenFile(comp, selectedFile, extraInfo, extension);
            return true;
        } else if (result == JFileChooser.CANCEL_OPTION) {
            // cancelled
        } else if (result == JFileChooser.ERROR_OPTION) {
            // error or dismissed
        }

        return false;
    }

    /**
     * Returns true if the file was saved, false if the user cancels the saving
     */
    public static boolean saveWithChooser(Composition comp) {
        initSaveChooser();

        String defaultExt = FileUtils
            .findExtension(comp.getName())
            .orElse(FileFormat.getLastOutput().toString());
        saveChooser.setFileFilter(getFileFilterForExtension(defaultExt));

        return showSaveChooserAndSaveComp(comp, null);
    }

    private static FileFilter getFileFilterForExtension(String ext) {
        ext = ext.toLowerCase();
        return switch (ext) {
            case "jpg", "jpeg" -> jpegFilter;
            case "png" -> pngFilter;
            case "bmp" -> bmpFilter;
            case "gif" -> gifFilter;
            case "pxc" -> pxcFilter;
            case "tif", "tiff" -> tiffFilter;
            default -> jpegFilter;
        };
    }

    private static void setDefaultOpenExtensions() {
        addDefaultFilters(openChooser);
    }

    public static void setDefaultSaveExtensions() {
        addDefaultFilters(saveChooser);
    }

    public static void setOnlyOneSaveExtension(FileFilter filter) {
        setupFilterToOnlyOneFormat(saveChooser, filter);
    }

    public static void setOnlyOneOpenExtension(FileFilter filter) {
        setupFilterToOnlyOneFormat(openChooser, filter);
    }

    private static void addDefaultFilters(JFileChooser chooser) {
        for (FileFilter filter : OPEN_SAVE_FILTERS) {
            chooser.addChoosableFileFilter(filter);
        }
    }

    private static void setupFilterToOnlyOneFormat(JFileChooser chooser,
                                                   FileFilter chosenFilter) {
        for (FileFilter filter : OPEN_SAVE_FILTERS) {
            if (filter != chosenFilter) {
                chooser.removeChoosableFileFilter(filter);
            }
        }

        chooser.setFileFilter(chosenFilter);
    }

    public static File selectSaveFileForSpecificFormat(FileFilter fileFilter) {
        File selectedFile = null;
        try {
            initSaveChooser();
            setupFilterToOnlyOneFormat(saveChooser, fileFilter);

            GlobalEvents.dialogOpened("Save");
            int status = saveChooser.showSaveDialog(PixelitorWindow.get());
            GlobalEvents.dialogClosed("Save");

            if (status == JFileChooser.APPROVE_OPTION) {
                selectedFile = saveChooser.getSelectedFile();
                Dirs.setLastSave(selectedFile.getParentFile());
            }
            if (status == JFileChooser.CANCEL_OPTION) {
                // save cancelled
                return null;
            }
            return selectedFile;
        } finally {
            setDefaultSaveExtensions();
        }
    }
}
