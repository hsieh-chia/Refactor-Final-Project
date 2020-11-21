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

package pixelitor.tools;

import pixelitor.OpenImages;
import pixelitor.gui.BlendingModePanel;
import pixelitor.layers.Drawable;
import pixelitor.layers.LayerMask;
import pixelitor.utils.debug.DebugNode;

import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Shape;

/**
 * An {@link AbstractBrushTool} tool that can have blending mode controls.
 * The blending mode controls are disabled when editing layer masks.
 */
public abstract class BlendingModeBrushTool extends AbstractBrushTool {
    private BlendingModePanel blendingModePanel;

    protected BlendingModeBrushTool(String name, char activationKeyChar,
                                    String iconFileName, String toolMessage,
                                    Cursor cursor, boolean canHaveSymmetry) {
        super(name, activationKeyChar, iconFileName, toolMessage, cursor, canHaveSymmetry);
        drawDestination = DrawDestination.TMP_LAYER;
    }

    @Override
    public void setupMaskEditing(boolean isMask) {
        if (isMask) {
            drawDestination = DrawDestination.DIRECT;
            blendingModePanel.setEnabled(false);
        } else {
            drawDestination = DrawDestination.TMP_LAYER;
            blendingModePanel.setEnabled(true);
        }
    }

    @Override
    protected void toolStarted() {
        super.toolStarted();

        var activeLayer = OpenImages.getActiveLayer();
        if(activeLayer != null) {
            setupMaskEditing(activeLayer.isMaskEditing());
        }
    }

    @Override
    public void trace(Drawable dr, Shape shape) {
        setupMaskEditing(dr instanceof LayerMask);
        super.trace(dr, shape);
    }

    @Override
    protected Composite getComposite() {
        return blendingModePanel.getComposite();
    }

    protected void addBlendingModePanel() {
        blendingModePanel = new BlendingModePanel(true);
        settingsPanel.add(blendingModePanel);
    }

    @Override
    public DebugNode getDebugNode() {
        var node = super.getDebugNode();

        node.addFloat("opacity", blendingModePanel.getOpacity());
        node.addQuotedString("blending Mode",
                blendingModePanel.getBlendingMode().toString());

        return node;
    }
}
