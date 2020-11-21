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

package pixelitor.utils.debug;

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.NewImage;
import pixelitor.OpenImages;
import pixelitor.colors.FillType;
import pixelitor.filters.Filter;
import pixelitor.gui.PixelitorWindow;
import pixelitor.gui.View;
import pixelitor.gui.utils.GUIUtils;
import pixelitor.layers.*;
import pixelitor.tools.Tools;
import pixelitor.tools.pen.Path;
import pixelitor.utils.*;

import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.NORTH;
import static javax.swing.BorderFactory.createEmptyBorder;
import static pixelitor.OpenImages.addAsNewComp;
import static pixelitor.OpenImages.findCompByName;
import static pixelitor.tools.pen.PenToolMode.EDIT;
import static pixelitor.utils.Threads.calledOutsideEDT;

/**
 * Debugging-related static utility methods
 */
public class Debug {
    private Debug() {
        // shouldn't be instantiated
    }

    public static String dateBufferTypeAsString(int type) {
        return switch (type) {
            case DataBuffer.TYPE_BYTE -> "BYTE";
            case DataBuffer.TYPE_USHORT -> "USHORT";
            case DataBuffer.TYPE_SHORT -> "SHORT";
            case DataBuffer.TYPE_INT -> "INT";
            case DataBuffer.TYPE_FLOAT -> "FLOAT";
            case DataBuffer.TYPE_DOUBLE -> "DOUBLE";
            case DataBuffer.TYPE_UNDEFINED -> "UNDEFINED";
            default -> "unrecognized (" + type + ")";
        };
    }

    static String transparencyAsString(int transparency) {
        return switch (transparency) {
            case Transparency.OPAQUE -> "OPAQUE";
            case Transparency.BITMASK -> "BITMASK";
            case Transparency.TRANSLUCENT -> "TRANSLUCENT";
            default -> "unrecognized (" + transparency + ")";
        };
    }

    public static String bufferedImageTypeAsString(int type) {
        return switch (type) {
            case BufferedImage.TYPE_3BYTE_BGR -> "3BYTE_BGR";
            case BufferedImage.TYPE_4BYTE_ABGR -> "4BYTE_ABGR";
            case BufferedImage.TYPE_4BYTE_ABGR_PRE -> "4BYTE_ABGR_PRE";
            case BufferedImage.TYPE_BYTE_BINARY -> "BYTE_BINARY";
            case BufferedImage.TYPE_BYTE_GRAY -> "BYTE_GRAY";
            case BufferedImage.TYPE_BYTE_INDEXED -> "BYTE_INDEXED";
            case BufferedImage.TYPE_CUSTOM -> "CUSTOM";
            case BufferedImage.TYPE_INT_ARGB -> "INT_ARGB";
            case BufferedImage.TYPE_INT_ARGB_PRE -> "INT_ARGB_PRE";
            case BufferedImage.TYPE_INT_BGR -> "INT_BGR";
            case BufferedImage.TYPE_INT_RGB -> "INT_RGB";
            case BufferedImage.TYPE_USHORT_555_RGB -> "USHORT_555_RGB";
            case BufferedImage.TYPE_USHORT_565_RGB -> "USHORT_565_RGB";
            case BufferedImage.TYPE_USHORT_GRAY -> "USHORT_GRAY";
            default -> "unrecognized (" + type + ")";
        };
    }

    static String colorSpaceTypeAsString(int type) {
        return switch (type) {
            case ColorSpace.TYPE_2CLR -> "2CLR";
            case ColorSpace.TYPE_3CLR -> "3CLR";
            case ColorSpace.TYPE_4CLR -> "4CLR";
            case ColorSpace.TYPE_5CLR -> "5CLR";
            case ColorSpace.TYPE_6CLR -> "6CLR";
            case ColorSpace.TYPE_7CLR -> "7CLR";
            case ColorSpace.TYPE_8CLR -> "8CLR";
            case ColorSpace.TYPE_9CLR -> "9CLR";
            case ColorSpace.TYPE_ACLR -> "ACLR";
            case ColorSpace.TYPE_BCLR -> "BCLR";
            case ColorSpace.TYPE_CCLR -> "CCLR";
            case ColorSpace.TYPE_CMY -> "CMY";
            case ColorSpace.TYPE_CMYK -> "CMYK";
            case ColorSpace.TYPE_DCLR -> "DCLR";
            case ColorSpace.TYPE_ECLR -> "ECLR";
            case ColorSpace.TYPE_FCLR -> "FCLR";
            case ColorSpace.TYPE_GRAY -> "GRAY";
            case ColorSpace.TYPE_HLS -> "HLS";
            case ColorSpace.TYPE_HSV -> "HSV";
            case ColorSpace.TYPE_Lab -> "Lab";
            case ColorSpace.TYPE_Luv -> "Luv";
            case ColorSpace.TYPE_RGB -> "RGB";
            case ColorSpace.TYPE_XYZ -> "XYZ";
            case ColorSpace.TYPE_YCbCr -> "YCbCr";
            case ColorSpace.TYPE_Yxy -> "Yxy";
            default -> "unrecognized (" + type + ")";
        };
    }

    public static boolean isRgbColorModel(ColorModel cm) {
        if (cm instanceof DirectColorModel
            && cm.getTransferType() == DataBuffer.TYPE_INT) {

            var dcm = (DirectColorModel) cm;
            return dcm.getRedMask() == 0x00FF0000
                && dcm.getGreenMask() == 0x0000FF00
                && dcm.getBlueMask() == 0x000000FF
                && (dcm.getNumComponents() == 3 || dcm.getAlphaMask() == 0xFF000000);
        }

        return false;
    }

    public static boolean isBgrColorModel(ColorModel cm) {
        if (cm instanceof DirectColorModel &&
            cm.getTransferType() == DataBuffer.TYPE_INT) {

            var dcm = (DirectColorModel) cm;
            return dcm.getRedMask() == 0x000000FF
                && dcm.getGreenMask() == 0x0000FF00
                && dcm.getBlueMask() == 0x00FF0000
                && (dcm.getNumComponents() == 3 || dcm.getAlphaMask() == 0xFF000000);
        }

        return false;
    }

    public static String mouseModifiers(MouseEvent e) {
        return modifierString(e.isControlDown(), e.isAltDown(), e.isShiftDown(),
            SwingUtilities.isRightMouseButton(e), e.isPopupTrigger());
    }

    public static String modifierString(boolean control, boolean alt, boolean shift,
                                        boolean right, boolean popup) {
        StringBuilder msg = new StringBuilder(25);
        if (control) {
            msg.append(Ansi.red("ctrl-"));
        }
        if (alt) {
            msg.append(Ansi.green("alt-"));
        }
        if (shift) {
            msg.append(Ansi.blue("shift-"));
        }
        if (right) {
            msg.append(Ansi.yellow("right-"));
        }
        if (popup) {
            msg.append(Ansi.cyan("popup-"));
        }
        return msg.toString();
    }

    public static void call(String... args) {
        call(false, args);
    }

    public static void callAndCaller(String... args) {
        call(true, args);
    }

    private static void call(boolean printCaller, String... args) {
        StackTraceElement[] fullTrace = new Throwable().getStackTrace();
        StackTraceElement me = fullTrace[2];

        String threadName;
        if (Threads.calledOnEDT()) {
            threadName = "EDT";
        } else {
            threadName = Threads.threadName();
        }

        String argsAsOneString;
        if (args.length == 0) {
            argsAsOneString = "";
        } else if (args.length == 1) {
            argsAsOneString = args[0];
        } else {
            argsAsOneString = Arrays.toString(args);
        }

        String className = me.getClassName();
        // strip the package name
        className = className.substring(className.lastIndexOf('.') + 1);

        if (printCaller) {
            StackTraceElement caller = fullTrace[3];
            String callerClassName = caller.getClassName();
            callerClassName = callerClassName.substring(callerClassName.lastIndexOf('.') + 1);

            System.out.printf("%s.%s(%s) on %s by %s.%s%n", className,
                me.getMethodName(), Ansi.yellow(argsAsOneString),
                threadName, callerClassName, caller.getMethodName());
        } else {
            System.out.printf("%s.%s(%s) on %s%n", className,
                me.getMethodName(), Ansi.yellow(argsAsOneString), threadName);
        }
    }

    public static String keyEvent(KeyEvent e) {
        String keyText = KeyEvent.getKeyText(e.getKeyCode());
        int modifiers = e.getModifiersEx();
        if (modifiers != 0) {
            String modifiersText = InputEvent.getModifiersExText(modifiers);
            if (keyText.equals(modifiersText)) { // the key itself is the modifier
                return modifiersText;
            }
            return modifiersText + "+" + keyText;
        }
        return keyText;
    }

    public static void image(Image img) {
        image(img, "Debug");
    }

    public static void image(Image img, String name) {
        // make sure the this is called on the EDT
        if (calledOutsideEDT()) {
            EventQueue.invokeLater(() -> image(img, name));
            return;
        }

        BufferedImage copy = ImageUtils.copyToBufferedImage(img);

        View previousView = OpenImages.getActiveView();

        findCompByName(name).ifPresentOrElse(
            comp -> replaceImageInDebugComp(comp, copy),
            () -> addAsNewComp(copy, null, name));

        if (previousView != null) {
            OpenImages.setActiveView(previousView, true);
        }
    }

    private static void replaceImageInDebugComp(Composition comp, BufferedImage copy) {
        Canvas canvas = comp.getCanvas();
        comp.getActiveDrawableOrThrow().setImage(copy);

        if (canvas.hasDifferentSizeThan(copy)) {
            canvas.changeSize(copy.getWidth(), copy.getHeight(), comp.getView());
        }

        comp.repaint();
    }

    public static void shape(Shape shape, String name) {
        // create a copy
        Path2D shapeCopy = new Path2D.Double(shape);

        Rectangle shapeBounds = shape.getBounds();
        int imgWidth = shapeBounds.x + shapeBounds.width + 50;
        int imgHeight = shapeBounds.y + shapeBounds.height + 50;

        BufferedImage debugImg = ImageUtils.createSysCompatibleImage(imgWidth, imgHeight);
        Drawer.on(debugImg)
            .fillWith(Color.WHITE)
            .useAA()
            .draw(g -> {
                g.setColor(Color.BLACK);
                g.setStroke(new BasicStroke(3));
                g.draw(shapeCopy);
            });

        image(debugImg, name);
    }

    public static void raster(Raster raster, String name) {
        ColorModel colorModel;
        int numBands = raster.getNumBands();

        if (numBands == 4) { // normal color image
            colorModel = new DirectColorModel(
                ColorSpace.getInstance(ColorSpace.CS_sRGB), 32,
                0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000,
                true, DataBuffer.TYPE_INT);
        } else if (numBands == 1) { // grayscale image
            int[] nBits = {8};
            colorModel = new ComponentColorModel(
                ColorSpace.getInstance(ColorSpace.CS_GRAY), nBits,
                false, true,
                Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        } else {
            throw new IllegalStateException("numBands = " + numBands);
        }

        Raster correctlyTranslated = raster.createChild(
            raster.getMinX(), raster.getMinY(),
            raster.getWidth(), raster.getHeight(),
            0, 0, null);
        BufferedImage debugImage = new BufferedImage(colorModel,
            (WritableRaster) correctlyTranslated, true, null);

        image(debugImage, name);
    }

    public static void rasterWithEmptySpace(Raster raster) {
        BufferedImage debugImage = new BufferedImage(
            raster.getMinX() + raster.getWidth(),
            raster.getMinY() + raster.getHeight(),
            BufferedImage.TYPE_4BYTE_ABGR_PRE);
        debugImage.setData(raster);

        image(debugImage);
    }

    public static void keepSwitchingToolsRandomly() {
        Runnable backgroundTask = () -> {
            //noinspection InfiniteLoopStatement
            while (true) {
                Utils.sleep(1, TimeUnit.SECONDS);

                Runnable changeToolOnEDTTask = () -> Tools.getRandomTool().activate();
                GUIUtils.invokeAndWait(changeToolOnEDTTask);
            }
        };
        new Thread(backgroundTask).start();
    }

    public static void dispatchKeyPress(PixelitorWindow pw, boolean ctrl, int keyCode, char keyChar) {
        int modifiers;
        if (ctrl) {
            modifiers = InputEvent.CTRL_DOWN_MASK;
        } else {
            modifiers = 0;
        }
        pw.dispatchEvent(new KeyEvent(pw, KeyEvent.KEY_PRESSED,
            System.currentTimeMillis(), modifiers, keyCode, keyChar));
    }

    public static void addTestPath() {
        var shape = new Rectangle2D.Double(100, 100, 300, 100);

        Path path = Shapes.shapeToPath(shape, OpenImages.getActiveView());

        Tools.PEN.setPath(path);
        Tools.PEN.startRestrictedMode(EDIT, false);
        Tools.PEN.activate();
    }

    public static void showAddTextLayerDialog() {
        AddTextLayerAction.INSTANCE.actionPerformed(null);
    }

    public static void addMaskAndShowIt() {
        AddLayerMaskAction.INSTANCE.actionPerformed(null);
        View view = OpenImages.getActiveView();
        Layer layer = view.getComp().getActiveLayer();
        MaskViewMode.SHOW_MASK.activate(view, layer);
    }

    public static void startFilter(Filter filter) {
        filter.startOn(OpenImages.getActiveDrawableOrThrow());
    }

    public static void addNewImageWithMask() {
        NewImage.addNewImage(FillType.WHITE, 600, 400, "Test");
        OpenImages.getActiveLayer().addMask(LayerMaskAddType.PATTERN);
    }

    public static void showInternalState() {
        AppNode node = new AppNode();
        String title = "Internal State";

        JTree tree = new JTree(node);

        JLabel explainLabel = new JLabel(
            "<html>If you are reporting a bug that cannot be reproduced," +
                "<br>please include the following information:");
        explainLabel.setBorder(createEmptyBorder(5, 5, 5, 5));

        JPanel form = new JPanel(new BorderLayout());
        form.add(explainLabel, NORTH);
        form.add(new JScrollPane(tree), CENTER);

        String text = node.toDetailedString();

        GUIUtils.showCopyTextToClipboardDialog(form, text, title);
    }
}
