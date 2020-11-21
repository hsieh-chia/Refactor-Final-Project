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
import pixelitor.OpenImages;
import pixelitor.automate.SingleDirChooser;
import pixelitor.gui.utils.Dialogs;
import pixelitor.layers.ImageLayer;
import pixelitor.layers.Layer;
import pixelitor.layers.LayerMask;
import pixelitor.layers.TextLayer;
import pixelitor.utils.Messages;
import pixelitor.utils.Utils;

import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static java.lang.String.format;
import static java.nio.file.Files.isWritable;
import static pixelitor.utils.Threads.*;

/**
 * Utility class with static methods related to opening and saving files.
 */
public class IO {
    private IO() {
    }

    public static CompletableFuture<Composition> openFileAsync(File file) {
        return loadCompAsync(file)
            .thenApplyAsync(OpenImages::addJustLoadedComp, onEDT)
            .whenComplete((comp, e) -> checkForIOProblems(e));
    }

    public static CompletableFuture<Composition> loadCompAsync(File file) {
        // if the file format is not recognized, this will still try to
        // read it in a single-layered format, which doesn't have to be JPG
        FileFormat format = FileFormat.fromFile(file).orElse(FileFormat.JPG);
        return format.readFrom(file);
    }

    public static CompletableFuture<Void> loadToNewImageLayerAsync(File file,
                                                                   Composition comp) {
        return CompletableFuture
            .supplyAsync(() -> TrackedIO.uncheckedRead(file), onIOThread)
            .thenAcceptAsync(img -> comp.addExternalImageAsNewLayer(
                img, file.getName(), "Dropped Layer"),
                onEDT)
            .whenComplete((v, e) -> checkForIOProblems(e));
    }

    /**
     * Utility method designed to be used with CompletableFuture.
     * Can be called on any thread.
     */
    public static void checkForIOProblems(Throwable e) {
        if (e == null) {
            // do nothing if the stage didn't complete exceptionally
            return;
        }
        if (e instanceof CompletionException) {
            // if the exception was thrown in a previous
            // stage, handle it the same way
            checkForIOProblems(e.getCause());
            return;
        }
        if (e instanceof DecodingException) {
            // decoding exceptions have an extra file field
            // that can be used for better error reporting
            DecodingException de = (DecodingException) e;
            File file = de.getFile();
            if (calledOnEDT()) {
                showDecodingError(file);
            } else {
                EventQueue.invokeLater(() -> showDecodingError(file));
            }
        } else {
            Messages.showExceptionOnEDT(e);
        }
    }

    private static void showDecodingError(File file) {
        String msg = format("Could not load \"%s\" as an image file.", file.getName());
        Messages.showError("Error", msg);
    }

    public static void save(boolean saveAs) {
        var comp = OpenImages.getActiveComp();
        save(comp, saveAs);
    }

    /**
     * Returns true if the file was saved,
     * false if the user cancels the saving or if it could not be saved
     */
    public static boolean save(Composition comp, boolean saveAs) {
        boolean needsFileChooser = saveAs || comp.getFile() == null;
        if (needsFileChooser) {
            return FileChoosers.saveWithChooser(comp);
        } else {
            File file = comp.getFile();
            if (file.exists()) { // if it was not deleted in the meantime...
                if (!isWritable(file.toPath())) {
                    Dialogs.showFileNotWritableDialog(file);
                    return false;
                }
            }
            Optional<FileFormat> fileFormat = FileFormat.fromFile(file);
            if (fileFormat.isPresent()) {
                var saveSettings = new SaveSettings(fileFormat.get(), file);
                comp.saveAsync(saveSettings, true);
                return true;
            } else {
                // the file was read from a file with an unsupported
                // extension, save it with a file chooser
                return FileChoosers.saveWithChooser(comp);
            }
        }
    }

    public static void saveImageToFile(BufferedImage image,
                                       SaveSettings saveSettings) {
        FileFormat format = saveSettings.getFormat();
        File selectedFile = saveSettings.getFile();

        Objects.requireNonNull(format);
        Objects.requireNonNull(selectedFile);
        Objects.requireNonNull(image);
        assert calledOutsideEDT() : "on EDT";

        try {
            if (format == FileFormat.JPG) {
                JpegSettings settings = JpegSettings.from(saveSettings);
                JpegOutput.write(image, selectedFile, settings.getJpegInfo());
            } else {
                TrackedIO.write(image, format.toString(), selectedFile);
            }
        } catch (IOException e) {
            if (e.getMessage().contains("another process")) {
                // handle here, because we have the file information
                showAnotherProcessErrorMsg(selectedFile);
            } else {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static void showAnotherProcessErrorMsg(File file) {
        String msg = format(
            "Can't save to%n%s%nbecause this file is being used by another program.",
            file.getAbsolutePath());

        EventQueue.invokeLater(() -> Messages.showError("Can't save", msg));
    }

    public static void openAllImagesInDir(File dir) {
        List<File> files = FileUtils.listSupportedInputFilesIn(dir);
        boolean found = false;
        for (File file : files) {
            found = true;
            openFileAsync(file);
        }
        if (!found) {
            String msg = format("<html>No supported image files found in <b>%s</b>.", dir.getName());
            Messages.showInfo("No files found", msg);
        }
    }

    public static void addAllImagesInDirAsLayers(File dir, Composition comp) {
        List<File> files = FileUtils.listSupportedInputFilesIn(dir);
        for (File file : files) {
            loadToNewImageLayerAsync(file, comp);
        }
    }

    public static void exportLayersToPNGAsync() {
        assert calledOnEDT() : threadInfo();

        boolean okPressed = SingleDirChooser.selectOutputDir();
        if (!okPressed) {
            return;
        }

        var comp = OpenImages.getActiveComp();

        CompletableFuture
            .supplyAsync(() -> exportLayersToPNG(comp), onIOThread)
            .thenAcceptAsync(numImg -> Messages.showInStatusBar(
                "Saved " + numImg + " images to <b>" + Dirs.getLastSave() + "</b>")
                , onEDT)
            .exceptionally(Messages::showExceptionOnEDT);
    }

    private static int exportLayersToPNG(Composition comp) {
        assert calledOutsideEDT() : "on EDT";

        int numSavedImages = 0;
        for (int layerIndex = 0; layerIndex < comp.getNumLayers(); layerIndex++) {
            Layer layer = comp.getLayer(layerIndex);
            if (layer instanceof ImageLayer) {
                ImageLayer imageLayer = (ImageLayer) layer;
                BufferedImage image = imageLayer.getImage();

                saveLayerImage(image, layer.getName(), layerIndex);
                numSavedImages++;
            } else if (layer instanceof TextLayer) {
                TextLayer textLayer = (TextLayer) layer;
                BufferedImage image = textLayer.createRasterizedImage();

                saveLayerImage(image, layer.getName(), layerIndex);
                numSavedImages++;
            }
            if (layer.hasMask()) {
                LayerMask mask = layer.getMask();
                BufferedImage image = mask.getImage();

                saveLayerImage(image, layer.getName() + "_mask", layerIndex);
                numSavedImages++;
            }
        }
        return numSavedImages;
    }

    private static void saveLayerImage(BufferedImage image,
                                       String layerName,
                                       int layerIndex) {
        assert calledOutsideEDT() : "on EDT";

        File outputDir = Dirs.getLastSave();
        String fileName = format("%03d_%s.png", layerIndex, Utils.toFileName(layerName));
        File file = new File(outputDir, fileName);

        saveImageToFile(image, new SaveSettings(FileFormat.PNG, file));
    }

    public static void saveCurrentImageInAllFormats() {
        boolean canceled = !SingleDirChooser.selectOutputDir();
        if (canceled) {
            return;
        }
        File saveDir = Dirs.getLastSave();
        if (saveDir != null) {
            var comp = OpenImages.getActiveComp();
            FileFormat[] fileFormats = FileFormat.values();
            for (FileFormat format : fileFormats) {
                File f = new File(saveDir, "all_formats." + format);
                var saveSettings = new SaveSettings(format, f);
                comp.saveAsync(saveSettings, false).join();
            }
        }
    }

    public static void saveJpegWithQuality(JpegInfo jpegInfo) {
        try {
            FileChoosers.initSaveChooser();
            FileChoosers.setOnlyOneSaveExtension(FileChoosers.jpegFilter);

            var comp = OpenImages.getActiveComp();
            FileChoosers.showSaveChooserAndSaveComp(comp, jpegInfo);
        } finally {
            FileChoosers.setDefaultSaveExtensions();
        }
    }

    static void saveToChosenFile(Composition comp, File file,
                                 Object extraInfo, String extension) {
        Dirs.setLastSave(file.getParentFile());

        FileFormat format = FileFormat.fromExtension(extension).orElseThrow();

        SaveSettings settings;
        if (extraInfo != null) {
            // currently the only type of extra information
            assert format == FileFormat.JPG : "format = " + format + ", extraInfo = " + extraInfo;
            JpegInfo jpegInfo = (JpegInfo) extraInfo;
            settings = new JpegSettings(jpegInfo, file);
        } else {
            settings = new SaveSettings(format, file);
        }

        comp.saveAsync(settings, true);
    }
}
