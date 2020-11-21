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

package pixelitor.tools.crop;

import pixelitor.Canvas;
import pixelitor.Composition;
import pixelitor.OpenImages;
import pixelitor.RunContext;
import pixelitor.compactions.Crop;
import pixelitor.filters.gui.RangeParam;
import pixelitor.gui.View;
import pixelitor.gui.utils.SliderSpinner;
import pixelitor.guides.GuidesRenderer;
import pixelitor.tools.ClipStrategy;
import pixelitor.tools.DragTool;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.tools.util.PRectangle;
import pixelitor.utils.Cursors;
import pixelitor.utils.Messages;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

import static java.awt.AlphaComposite.SRC_OVER;
import static java.awt.Color.BLACK;
import static pixelitor.gui.utils.SliderSpinner.TextPosition.WEST;
import static pixelitor.tools.crop.CropToolState.*;

/**
 * The crop tool
 */
public class CropTool extends DragTool {
    private CropToolState state = INITIAL;

    private CropBox cropBox;

    private final RangeParam maskOpacity = new RangeParam("Mask Opacity (%)", 0, 75, 100);

    private Composite hideComposite = AlphaComposite.getInstance(
            SRC_OVER, maskOpacity.getPercentageValF());

    private final JButton cancelButton = new JButton("Cancel");
    private JButton cropButton;

    private final JLabel widthLabel = new JLabel("Width:");
    private final JLabel heightLabel = new JLabel("Height:");
    private JSpinner widthSpinner;
    private JSpinner heightSpinner;
    private JComboBox<CompositionGuideType> guidesCB;

    private JCheckBox allowGrowingCB;
    private JCheckBox deleteCroppedPixelsCB;

    private final CompositionGuide compositionGuide;

    public CropTool() {
        super("Crop", 'C', "crop_tool_icon.png",
                "<b>drag</b> to start or <b>Alt-drag</b> to start form the center. " +
                        "After the handles appear: " +
                        "<b>Shift-drag</b> keeps the aspect ratio, " +
                        "<b>Double-click</b> crops, <b>Esc</b> cancels.",
                Cursors.DEFAULT, false,
                true, false, ClipStrategy.CUSTOM);
        spaceDragStartPoint = true;

        GuidesRenderer renderer = GuidesRenderer.CROP_GUIDES_INSTANCE.get();
        compositionGuide = new CompositionGuide(renderer);
    }

    /**
     * Initialize settings panel controls
     */
    @Override
    public void initSettingsPanel() {
        addMaskOpacitySelector();
        settingsPanel.addSeparator();

        addGuidesSelector();
        settingsPanel.addSeparator();

        addCropSizeControls();
        settingsPanel.addSeparator();

        addCropButton();
        addCancelButton();

        settingsPanel.addSeparator();

        addCropControlCheckboxes();

        enableCropActions(false);

        if (RunContext.isDevelopment()) {
            JButton b = new JButton("Dump State");
            b.addActionListener(e -> {
                View view = OpenImages.getActiveView();
                Canvas canvas = view.getCanvas();
                System.out.println("CropTool::actionPerformed: canvas = " + canvas);
                System.out.println("CropTool::initSettingsPanel: state = " + state);
                System.out.println("CropTool::initSettingsPanel: cropBox = " + cropBox);
            });
            settingsPanel.add(b);
        }
    }

    private void addMaskOpacitySelector() {
        maskOpacity.addChangeListener(e -> maskOpacityChanged());
        SliderSpinner maskOpacitySpinner = new SliderSpinner(
                maskOpacity, WEST, false);
        settingsPanel.add(maskOpacitySpinner);
    }

    private void maskOpacityChanged() {
        float alpha = maskOpacity.getPercentageValF();
        // because of a swing bug, the slider can get out of range
        if (alpha < 0.0f) {
            alpha = 0.0f;
            maskOpacity.setValue(0);
        } else if (alpha > 1.0f) {
            alpha = 1.0f;
            maskOpacity.setValue(100);
        }
        hideComposite = AlphaComposite.getInstance(SRC_OVER, alpha);
        OpenImages.repaintActive();
    }

    private void addGuidesSelector() {
        guidesCB = new JComboBox<>(CompositionGuideType.values());
        guidesCB.setToolTipText("<html>Composition guides." +
                "<br><br>Press <b>O</b> to select the next guide." +
                "<br>Press <b>Shift-O</b> to change the orientation.");
        guidesCB.setMaximumRowCount(guidesCB.getItemCount());
        guidesCB.addActionListener(e -> OpenImages.repaintActive());
        settingsPanel.addComboBox("Guides:", guidesCB, "guidesCB");
    }

    private void addCropSizeControls() {
        ChangeListener whChangeListener = e -> {
            if (state == TRANSFORM && !cropBox.isAdjusting()) {
                cropBox.setImSize(
                        (int) widthSpinner.getValue(),
                        (int) heightSpinner.getValue(),
                        OpenImages.getActiveView()
                );
            }
        };

        // add crop width spinner
        widthSpinner = createSpinner(whChangeListener, Canvas.MAX_WIDTH,
                "Width of the cropped image (px)");
        settingsPanel.add(widthLabel);
        settingsPanel.add(widthSpinner);

        // add crop height spinner
        heightSpinner = createSpinner(whChangeListener, Canvas.MAX_HEIGHT,
                "Height of the cropped image (px)");
        settingsPanel.add(heightLabel);
        settingsPanel.add(heightSpinner);
    }

    private static JSpinner createSpinner(ChangeListener whChangeListener, int max, String toolTip) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(
                0, 0, max, 1));
        spinner.addChangeListener(whChangeListener);
        spinner.setToolTipText(toolTip);
        // In fact setting it to 3 columns seems enough
        // for the range 1-9999, but leave it as 4 for safety
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(4);
        return spinner;
    }

    private void addCropControlCheckboxes() {
        deleteCroppedPixelsCB = settingsPanel.addCheckBox(
                "Delete Cropped Pixels", true, "deleteCroppedPixelsCB",
                "If not checked, only the canvas gets smaller");

        allowGrowingCB = settingsPanel.addCheckBox(
                "Allow Growing", false, "allowGrowingCB",
                "Enables the enlargement of the canvas");
    }

    private void addCropButton() {
        cropButton = new JButton("Crop");
        cropButton.addActionListener(e -> executeCropCommand());
        settingsPanel.add(cropButton);
    }

    private void addCancelButton() {
        cancelButton.addActionListener(e -> executeCancelCommand());
        settingsPanel.add(cancelButton);
    }

    @Override
    public void mouseClicked(PMouseEvent e) {
        if (e.getClickCount() == 2 && !e.isConsumed()) {
            mouseDoubleClicked(e);
        }
    }

    private void mouseDoubleClicked(PMouseEvent e) {
        // if user double clicked inside selection then accept cropping

        if (state != TRANSFORM) {
            return;
        }

        if (!cropBox.getRect().containsCo(e.getPoint())) {
            return;
        }

        e.consume();
        executeCropCommand();
    }

    @Override
    public void dragStarted(PMouseEvent e) {
        // in case of crop/image change the ended is set to
        // true even if the tool is not ended.
        // if a new drag is started, then reset it
        ended = false;

        setState(state.getNextAfterMousePressed());

        if (state == TRANSFORM) {
            assert cropBox != null;
            cropBox.mousePressed(e);
            enableCropActions(true);
        } else if (state == USER_DRAG) {
            enableCropActions(true);
        }
    }

    @Override
    public void ongoingDrag(PMouseEvent e) {
        if (state == TRANSFORM) {
            cropBox.mouseDragged(e);
        } else if (userDrag != null) {
            userDrag.setStartFromCenter(e.isAltDown());
        }

        PRectangle cropRect = getCropRect();
        if (cropRect != null) {
            updateSizeSettings(cropRect);
        }

        // in the USER_DRAG state this will also
        // cause the painting of the darkening overlay
        e.repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e, View view) {
        super.mouseMoved(e, view);
        if (state == TRANSFORM) {
            cropBox.mouseMoved(e, view);
        }
    }

    @Override
    public void dragFinished(PMouseEvent e) {
        var comp = e.getComp();
        comp.imageChanged();

        switch (state) {
            case INITIAL:
                break;
            case USER_DRAG:
                if (cropBox != null) {
                    throw new IllegalStateException();
                }

                Rectangle r = userDrag.toCoRect();
                PRectangle rect = PRectangle.positiveFromCo(r, e.getView());

                cropBox = new CropBox(rect, e.getView());

                setState(TRANSFORM);
                break;
            case TRANSFORM:
                if (cropBox == null) {
                    throw new IllegalStateException();
                }
                cropBox.mouseReleased(e);
                break;
        }
    }

    @Override
    public void paintOverImage(Graphics2D g2, Composition comp,
                               AffineTransform imageTransform) {
        if (ended) {
            return;
        }
        View view = comp.getView();
        if (!view.isActive()) {
            return;
        }
        PRectangle cropRect = getCropRect();
        if (cropRect == null) {
            return;
        }

        // TODO done for compatibility. The whole code should be re-evaluated
        AffineTransform componentTransform = g2.getTransform();
        g2.setTransform(imageTransform);

        // paint the semi-transparent dark area outside the crop rectangle
        Shape origClip = g2.getClip();  // save for later use

        Canvas canvas = comp.getCanvas();
        Rectangle canvasBounds = canvas.getBounds();

        // Similar to ClipStrategy.FULL, but we need some intermediary variables

        Rectangle coVisiblePart = view.getVisiblePart();
        // ...but first get this to image space...
        Rectangle2D imVisiblePart = view.componentToImageSpace(coVisiblePart);
        // ... and now we can intersect
        Rectangle2D canvasImgIntersection = canvasBounds.createIntersection(imVisiblePart);
        Path2D darkAreaClip = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        darkAreaClip.append(canvasImgIntersection, false);

        Rectangle2D cropRectIm = cropRect.getIm();
        darkAreaClip.append(cropRectIm, false);
        g2.setClip(darkAreaClip);

        Color origColor = g2.getColor();
        g2.setColor(BLACK);

        Composite origComposite = g2.getComposite();
        g2.setComposite(hideComposite);

        g2.fill(canvasImgIntersection);

        g2.setColor(origColor);
        g2.setComposite(origComposite);

        g2.setTransform(componentTransform);
        if (state == TRANSFORM) {
            // Paint the handles.
            // The zooming is temporarily reset because the transformSupport works in component space

            // prevents drawing outside the InternalImageFrame/CompositionView
            // it is important to call this AFTER setting the unscaled transform
            g2.setClip(coVisiblePart);

            // draw composition guides
            compositionGuide.setType((CompositionGuideType) guidesCB.getSelectedItem());
            compositionGuide.draw(cropRect.getCo(), g2);

            cropBox.paintHandles(g2);
        }

        g2.setClip(origClip);
    }

    private void enableCropActions(boolean b) {
        widthLabel.setEnabled(b);
        heightSpinner.setEnabled(b);

        heightLabel.setEnabled(b);
        widthSpinner.setEnabled(b);

        cropButton.setEnabled(b);
        cancelButton.setEnabled(b);
    }

    /**
     * Update the settings panel after the crop size changes
     */
    private void updateSizeSettings(PRectangle cropRect) {
        Rectangle2D imRect = cropRect.getIm();
        int width = (int) imRect.getWidth();
        int height = (int) imRect.getHeight();

        widthSpinner.setValue(width);
        heightSpinner.setValue(height);
    }

    /**
     * Returns the crop rectangle
     */
    public PRectangle getCropRect() {
        if (state == USER_DRAG) {
            return userDrag.toPosPRect();
        } else if (state == TRANSFORM) {
            return cropBox.getRect();
        }
        // initial state
        return null;
    }

    @Override
    protected void toolEnded() {
        super.toolEnded();
        resetInitialState();
    }

    @Override
    public void resetInitialState() {
        ended = true;
        cropBox = null;
        setState(INITIAL);

        enableCropActions(false);

        heightSpinner.setValue(0);
        widthSpinner.setValue(0);

        OpenImages.repaintActive();
        OpenImages.setCursorForAll(Cursors.DEFAULT);
    }

    private void setState(CropToolState newState) {
        state = newState;
    }

    @Override
    public void compReplaced(Composition oldComp, Composition newComp, boolean reloaded) {
        if (reloaded) {
            resetInitialState();
        }
    }

    @Override
    public void coCoordsChanged(View view) {
        if (cropBox != null && state == TRANSFORM) {
            cropBox.coCoordsChanged(view);
        }
    }

    @Override
    public void imCoordsChanged(Composition comp, AffineTransform at) {
        if (cropBox != null && state == TRANSFORM) {
            cropBox.imCoordsChanged(comp, at);
        }
    }

    @Override
    public boolean arrowKeyPressed(ArrowKey key) {
        if (state == TRANSFORM) {
            View view = OpenImages.getActiveView();
            if (view != null) {
                cropBox.arrowKeyPressed(key, view);
                return true;
            }
        }
        return false;
    }

    @Override
    public void escPressed() {
        executeCancelCommand();
    }

    private void executeCropCommand() {
        if (state != TRANSFORM) {
            return;
        }

        Rectangle2D cropRect = getCropRect().getIm();
        Crop.toolCropActiveImage(cropRect,
                allowGrowingCB.isSelected(),
                deleteCroppedPixelsCB.isSelected());
        resetInitialState();
    }

    private void executeCancelCommand() {
        if (state != TRANSFORM) {
            return;
        }

        resetInitialState();
        Messages.showPlainInStatusBar("Crop canceled.");
    }

    @Override
    public void otherKeyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            executeCropCommand();
            e.consume();
        } else if (e.getKeyCode() == KeyEvent.VK_O) {
            if (e.isControlDown()) {
                // ignore Ctrl-O see issue #81
                return;
            }
            if (e.isShiftDown()) {
                // Shift-O: change the orientation
                // within the current composition guide family
                if (state == TRANSFORM) {
                    int o = compositionGuide.getOrientation();
                    compositionGuide.setOrientation(o + 1);
                    OpenImages.repaintActive();
                    e.consume();
                }
            } else {
                // O: advance to the next composition guide
                selectTheNextCompositionGuide();
                e.consume();
            }
        }
    }

    private void selectTheNextCompositionGuide() {
        int index = guidesCB.getSelectedIndex();
        int itemCount = guidesCB.getItemCount();
        int nextIndex;
        if (index == itemCount - 1) {
            nextIndex = 0;
        } else {
            nextIndex = index + 1;
        }
        guidesCB.setSelectedIndex(nextIndex);
    }

    @Override
    public DebugNode getDebugNode() {
        var node = super.getDebugNode();

        node.addFloat("mask opacity", maskOpacity.getPercentageValF());
        node.addBoolean("allow growing", allowGrowingCB.isSelected());
        node.addString("state", state.toString());

        return node;
    }
}
