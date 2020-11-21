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
import pixelitor.utils.ImageUtils;
import pixelitor.utils.Utils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static pixelitor.utils.Threads.onEDT;
import static pixelitor.utils.Threads.onIOThread;

/**
 * The input and output file formats
 */
public enum FileFormat {
    JPG(false, false) {
    }, PNG(false, true) {
    }, TIFF(false, true) {
    }, GIF(false, true) {
    }, BMP(false, false) {
    }, PXC(true, true) {
        @Override
        public Runnable getSaveTask(Composition comp, SaveSettings settings) {
            return () -> PXCFormat.write(comp, settings.getFile());
        }

        @Override
        public CompletableFuture<Composition> readFrom(File file) {
            return CompletableFuture.supplyAsync(
                Utils.toSupplier(() -> PXCFormat.read(file)), onIOThread);
        }
    }, ORA(true, true) {
        @Override
        public Runnable getSaveTask(Composition comp, SaveSettings settings) {
            return () -> OpenRaster.uncheckedWrite(comp, settings.getFile(), false);
        }

        @Override
        public CompletableFuture<Composition> readFrom(File file) {
            return CompletableFuture.supplyAsync(
                Utils.toSupplier(() -> OpenRaster.read(file)), onIOThread);
        }
    };

    private final boolean supportsMultipleLayers;
    private final boolean supportsAlpha;

    FileFormat(boolean layered, boolean hasAlpha) {
        supportsMultipleLayers = layered;
        supportsAlpha = hasAlpha;
    }

    public Runnable getSaveTask(Composition comp, SaveSettings settings) {
        assert !supportsMultipleLayers; // overwritten for multi-layered formats

        return () -> saveSingleLayered(comp, settings);
    }

    public CompletableFuture<Composition> readFrom(File file) {
        // overwritten for multi-layered formats
        return readSimpleFrom(file);
    }

    /**
     * Loads a composition from a file with a single-layer image format
     */
    private static CompletableFuture<Composition> readSimpleFrom(File file) {
        return CompletableFuture.supplyAsync(() -> TrackedIO.uncheckedRead(file), onIOThread)
            .thenApplyAsync(img -> Composition.fromImage(img, file, null), onEDT);
    }

    private void saveSingleLayered(Composition comp, SaveSettings settings) {
        BufferedImage img = comp.getCompositeImage();
        if (!supportsAlpha) {
            // no alpha support, convert first to RGB
            img = ImageUtils.convertToRGB(img, false);
        } else if (this == GIF) {
            img = ImageUtils.convertToIndexed(img, false);
        }
        IO.saveImageToFile(img, settings);
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

    public static Optional<FileFormat> fromFile(File file) {
        String fileName = file.getName();
        String extension = FileUtils.findExtension(fileName).orElse("");
        return fromExtension(extension);
    }

    public static Optional<FileFormat> fromExtension(String extension) {
        String extLC = extension.toLowerCase();
        return switch (extLC) {
            case "jpg", "jpeg" -> Optional.of(JPG);
            case "png" -> Optional.of(PNG);
            case "bmp" -> Optional.of(BMP);
            case "gif" -> Optional.of(GIF);
            case "pxc" -> Optional.of(PXC);
            case "ora" -> Optional.of(ORA);
            case "tif", "tiff" -> Optional.of(TIFF);
            default -> Optional.empty();
        };
    }

    private static volatile FileFormat lastOutput = JPG;

    public static FileFormat getLastOutput() {
        return lastOutput;
    }

    public static void setLastOutput(FileFormat format) {
        lastOutput = format;
    }
}
