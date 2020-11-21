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

import org.jdesktop.swingx.combobox.EnumComboBoxModel;
import pixelitor.Composition;
import pixelitor.ConsistencyChecks;
import pixelitor.OpenImages;
import pixelitor.RunContext;
import pixelitor.gui.GUIText;
import pixelitor.gui.View;
import pixelitor.selection.*;
import pixelitor.tools.util.ArrowKey;
import pixelitor.tools.util.DragDisplayType;
import pixelitor.tools.util.PMouseEvent;
import pixelitor.utils.Cursors;
import pixelitor.utils.Messages;
import pixelitor.utils.VisibleForTesting;
import pixelitor.utils.debug.DebugNode;

import javax.swing.*;

import static pixelitor.selection.ShapeCombination.*;

/**
 * The selection tool
 */
public class SelectionTool extends DragTool {
    private static final String HELP_TEXT = "<b>click and drag</b> creates a selection, " +
        "<b>Space-drag</b> moves it. " +
        "<b>Shift-drag</b> adds to an existing selection, " +
        "<b>Alt-drag</b> removes from it, <b>Shift-Alt-drag</b> intersects.";
    private static final String POLY_HELP_TEXT = "Polygonal selection: " +
        "<b>click</b> to add points, " +
        "<b>double-click</b> (or <b>right-click</b>) to close the selection." +
        "<b>Shift</b> adds to an existing selection, " +
        "<b>Alt</b> removes from it, <b>Shift+Alt</b> intersects.";
    private static final String FREEHAND_HELP_TEXT = "Freehand selection: " +
        "simply drag around the area that you want to select. " +
        "<b>Shift-drag</b> adds to an existing selection, " +
        "<b>Alt-drag</b> removes from it, <b>Shift-Alt-drag</b> intersects.";

    private boolean altMeansSubtract = false;
    private ShapeCombination originalShapeCombination;

    private SelectionBuilder selectionBuilder;
    private boolean polygonal = false;
    private boolean displayWidthHeight = true;

    private final EnumComboBoxModel<SelectionType> typeModel
        = new EnumComboBoxModel<>(SelectionType.class);
    private final EnumComboBoxModel<ShapeCombination> interactionModel
        = new EnumComboBoxModel<>(ShapeCombination.class);

    SelectionTool() {
        super("Selection", 'M', "selection_tool_icon.png",
            HELP_TEXT, Cursors.DEFAULT, false,
            true, false, ClipStrategy.FULL);
        spaceDragStartPoint = true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initSettingsPanel() {
        var typeCB = new JComboBox<SelectionType>(typeModel);
        typeCB.addActionListener(e -> selectionTypeChanged());
        settingsPanel.addComboBox(GUIText.TYPE + ":", typeCB, "typeCB");

        settingsPanel.addSeparator();

        var interactionCB = new JComboBox<ShapeCombination>(interactionModel);
        settingsPanel.addComboBox("New Selection:",
            interactionCB, "interactionCB");

        settingsPanel.addSeparator();

        settingsPanel.addButton(SelectionActions.getCrop(),
            "cropButton", "Crop using the current selection");

        settingsPanel.addButton(SelectionActions.getConvertToPath(),
            "toPathButton", "Convert the selection to a path");
    }

    private void selectionTypeChanged() {
        stopBuildingSelection();

        SelectionType type = getSelectionType();
        polygonal = type == SelectionType.POLYGONAL_LASSO;
        displayWidthHeight = type.displayWidthHeight();

        if (polygonal) {
            Messages.showInStatusBar(POLY_HELP_TEXT);
        } else if (type == SelectionType.LASSO) {
            Messages.showInStatusBar(FREEHAND_HELP_TEXT);
        } else {
            Messages.showInStatusBar("Selection Tool: " + HELP_TEXT);
        }
    }

    @Override
    public void dragStarted(PMouseEvent e) {
        if (polygonal) {
            return; // ignore mouse pressed
        }

        setupInteractionWithKeyModifiers(e);

        selectionBuilder = new SelectionBuilder(getSelectionType(),
            getCurrentInteraction(), e.getComp());
    }

    @Override
    public void ongoingDrag(PMouseEvent e) {
        if (polygonal) {
            return; // ignore dragging
        }
        if (selectionBuilder == null) {
            // the image was changed so start again
            dragStarted(e);
        }

        altDown = e.isAltDown();
        boolean startFromCenter = !altMeansSubtract && altDown;
        if (!altDown) {
            altMeansSubtract = false;
        }

        userDrag.setStartFromCenter(startFromCenter);
        selectionBuilder.updateBuiltSelection(userDrag.toImDrag(), e.getComp());
    }

    @Override
    public void dragFinished(PMouseEvent e) {
        if (userDrag.isClick() && !polygonal) { // will be handled by mouseClicked
            restoreInteraction();
            return;
        }

        var comp = e.getComp();
        Selection builtSelection = comp.getBuiltSelection();
        if (builtSelection == null && !polygonal) {
            // can happen, if we called stopBuildingSelection()
            // for some exceptional reason
            return;
        }

        if (polygonal) {
            polygonalDragFinished(e);
        } else {
            notPolygonalDragFinished(e);
        }

        altMeansSubtract = false;

        assert ConsistencyChecks.selectionShapeIsNotEmpty(comp) :
            "selection is empty";
        assert ConsistencyChecks.selectionIsInsideCanvas(comp) :
            "selection is outside";
    }

    private void polygonalDragFinished(PMouseEvent e) {
        var comp = e.getComp();
        if (selectionBuilder == null) {
            setupInteractionWithKeyModifiers(e);
            selectionBuilder = new SelectionBuilder(getSelectionType(),
                getCurrentInteraction(), comp);
            selectionBuilder.updateBuiltSelection(e, comp);
            restoreInteraction();
        } else {
            selectionBuilder.updateBuiltSelection(e, comp);
            if (e.isRight()) {
                selectionBuilder.combineShapes(comp);
                stopBuildingSelection();
            }
        }
    }

    private void notPolygonalDragFinished(PMouseEvent e) {
        var comp = e.getComp();
        restoreInteraction();

        boolean startFromCenter = !altMeansSubtract && e.isAltDown();
        userDrag.setStartFromCenter(startFromCenter);

        selectionBuilder.updateBuiltSelection(userDrag.toImDrag(), comp);
        selectionBuilder.combineShapes(comp);
        stopBuildingSelection();

        assert !comp.hasBuiltSelection();
    }

    @Override
    public void mouseClicked(PMouseEvent e) {
        var comp = e.getComp();
        if (polygonal) {
            if (selectionBuilder != null && e.getClickCount() > 1) {
                // finish polygonal for double-click
                selectionBuilder.updateBuiltSelection(e, comp);
                selectionBuilder.combineShapes(comp);
                stopBuildingSelection();
            } else {
                // ignore otherwise: will be handled in mouse released
            }
            return;
        }

        super.mouseClicked(e);

        cancelSelection(comp);
    }

    private void cancelSelection(Composition comp) {
        deselect(comp, true);
        assert !comp.hasBuiltSelection() : "built selection is = " + comp.getBuiltSelection();
        assert !comp.hasSelection() : "selection is = " + comp.getSelection();

        altMeansSubtract = false;

        if (RunContext.isDevelopment()) {
            ConsistencyChecks.selectionActionsEnabledCheck(comp);
        }
    }

    private static void deselect(Composition comp, boolean addToHistory) {
        if (comp.hasSelection() || comp.hasBuiltSelection()) {
            comp.deselect(addToHistory);
        }
    }

    @Override
    public void escPressed() {
        // pressing Esc should work the same as clicking outside the selection
        OpenImages.onActiveComp(this::cancelSelection);
    }

    @Override
    public void altPressed() {
        if (!altDown && !altMeansSubtract && userDrag != null && userDrag.isDragging()) {
            userDrag.setStartFromCenter(true);
            var comp = OpenImages.getActiveComp();
            selectionBuilder.updateBuiltSelection(userDrag.toImDrag(), comp);
        }
        altDown = true;
    }

    @Override
    public void altReleased() {
        if (!altMeansSubtract && userDrag != null && userDrag.isDragging()) {
            userDrag.setStartFromCenter(false);
            var comp = OpenImages.getActiveComp();
            selectionBuilder.updateBuiltSelection(userDrag.toImDrag(), comp);
        }
        altDown = false;
    }

    @Override
    public boolean arrowKeyPressed(ArrowKey key) {
        View view = OpenImages.getActiveView();
        if (view != null) {
            var comp = view.getComp();
            var selection = comp.getSelection();
            if (selection != null) {
                selection.nudge(key.getTransform());
                return true;
            }
        }
        return false;
    }

    @Override
    public DragDisplayType getDragDisplayType() {
        if (displayWidthHeight) {
            return DragDisplayType.WIDTH_HEIGHT;
        }
        return DragDisplayType.NONE;
    }

    private void setupInteractionWithKeyModifiers(PMouseEvent e) {
        boolean shiftDown = e.isShiftDown();
        altDown = e.isAltDown();

        altMeansSubtract = altDown;

        if (shiftDown || altDown) {
            originalShapeCombination = getCurrentInteraction();
            if (shiftDown) {
                if (altDown) {
                    setCurrentInteraction(INTERSECT);
                } else {
                    setCurrentInteraction(ADD);
                }
            } else if (altDown) {
                setCurrentInteraction(SUBTRACT);
            }
        }
    }

    private void restoreInteraction() {
        if (originalShapeCombination != null) {
            setCurrentInteraction(originalShapeCombination);
            originalShapeCombination = null;
        }
    }

    @Override
    public void allViewsClosed() {
        // ignore
    }

    @Override
    public void viewActivated(View oldCV, View newCV) {
        stopBuildingSelection();
    }

    private void stopBuildingSelection() {
        if (selectionBuilder != null) {
            var comp = OpenImages.getActiveComp();
            selectionBuilder.cancelIfNotFinished(comp);
            selectionBuilder = null;
        }
    }

    @Override
    protected void toolEnded() {
        super.toolEnded();

        // otherwise in polygonal mode half-built selections
        // remain visible after switching to another tool
        stopBuildingSelection();
    }

    @VisibleForTesting
    public SelectionType getSelectionType() {
        return typeModel.getSelectedItem();
    }

    @VisibleForTesting
    public ShapeCombination getCurrentInteraction() {
        return interactionModel.getSelectedItem();
    }

    private void setCurrentInteraction(ShapeCombination interaction) {
        interactionModel.setSelectedItem(interaction);
    }

    @Override
    public String getStateInfo() {
        return getSelectionType() + ", " + getCurrentInteraction();
    }

    @Override
    public DebugNode getDebugNode() {
        var node = super.getDebugNode();

        node.addString("type", getSelectionType().toString());
        node.addString("interaction", getCurrentInteraction().toString());

        return node;
    }
}
