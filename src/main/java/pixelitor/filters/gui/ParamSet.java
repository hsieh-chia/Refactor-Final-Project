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

package pixelitor.filters.gui;

import pixelitor.filters.Filter;
import pixelitor.utils.Icons;
import pixelitor.utils.Utils;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static pixelitor.filters.gui.FilterSetting.EnabledReason.FINAL_ANIMATION_SETTING;

/**
 * A fixed set of filter parameter objects
 */
public class ParamSet {
    private List<FilterParam> paramList = new ArrayList<>();
    private final List<FilterButtonModel> actionList = new ArrayList<>(3);
    private ParamAdjustmentListener adjustmentListener;
    private Runnable beforeResetAction;
    private FilterState[] builtinPresets;
    private boolean nonTrivialFilter;

    public ParamSet(FilterParam... params) {
        paramList.addAll(List.of(params));
    }

    public ParamSet(FilterParam param) {
        paramList.add(param);
    }

    public ParamSet(List<FilterParam> params) {
        paramList.addAll(params);
    }

    /**
     * Adds the given parameters after the existing ones
     */
    public void addParams(FilterParam[] params) {
        Collections.addAll(paramList, params);
    }

    /**
     * Adds the given parameters before the existing ones
     */
    public void addParamsToFront(FilterParam[] params) {
        List<FilterParam> old = paramList;
        paramList = new ArrayList<>(params.length + old.size());
        Collections.addAll(paramList, params);
        paramList.addAll(old);
    }

    public void insertParamAtIndex(FilterParam param, int index) {
        paramList.add(index, param);
    }

    public void insertAction(FilterButtonModel action, int index) {
        actionList.add(index, action);
    }

    public ParamSet withActions(FilterButtonModel... actions) {
        actionList.addAll(List.of(actions));
        return this;
    }

    public ParamSet withActionsAtFront(FilterButtonModel... actions) {
        for (FilterButtonModel action : actions) {
            actionList.add(0, action);
        }
        return this;
    }

    public ParamSet withAction(FilterButtonModel action) {
        actionList.add(action);
        return this;
    }

    public ParamSet addCommonActions(FilterButtonModel... actions) {
        for (FilterButtonModel action : actions) {
            if (action != null) {
                actionList.add(action);
            }
        }

        // no need for "randomize"/"reset all"
        // if the filter has only one parameter...
        nonTrivialFilter = paramList.size() > 1;

        if (!nonTrivialFilter) {
            FilterParam param = paramList.get(0);
            // ...except if that single parameter is grouped...
            if (param instanceof GroupedRangeParam) {
                nonTrivialFilter = true;
            }
            // ...or it is a gradient param
            if (param instanceof GradientParam) {
                nonTrivialFilter = true;
            }
        }
        if (nonTrivialFilter) {
            addRandomizeAction();
            addResetAllAction();
        }
        return this;
    }

    private void addRandomizeAction() {
        var randomizeAction = new FilterButtonModel("Randomize Settings",
            this::randomize,
            Icons.getDiceIcon(),
            "Randomize the settings for this filter.",
            "randomize");
        actionList.add(randomizeAction);
    }

    private void addResetAllAction() {
        var resetAllAction = new FilterButtonModel("Reset All",
            this::reset,
            Icons.getWestArrowIcon(),
            Resettable.RESET_ALL_TOOLTIP,
            "resetAll");
        actionList.add(resetAllAction);
    }

    /**
     * Allows registering an action that will run before "reset all"
     */
    public void setBeforeResetAction(Runnable beforeResetAction) {
        this.beforeResetAction = beforeResetAction;
    }

    /**
     * Resets all params without triggering the filter
     */
    public void reset() {
        if (beforeResetAction != null) {
            beforeResetAction.run();
        }
        for (FilterParam param : paramList) {
            param.reset(false);
        }
    }

    public void randomize() {
        long before = Filter.runCount;

        paramList.forEach(FilterParam::randomize);

        // the filter is not supposed to be triggered
        long after = Filter.runCount;
        assert before == after : "before = " + before + ", after = " + after;
    }

    public void runFilter() {
        if (adjustmentListener != null) {
            adjustmentListener.paramAdjusted();
        }
    }

    public void setAdjustmentListener(ParamAdjustmentListener listener) {
        adjustmentListener = listener;

        for (FilterParam param : paramList) {
            param.setAdjustmentListener(listener);
        }
        for (FilterButtonModel action : actionList) {
            action.setAdjustmentListener(listener);
        }
    }

    public void considerImageSize(Rectangle bounds) {
        for (FilterParam param : paramList) {
            param.considerImageSize(bounds);
        }
    }

    public FilterState copyState(boolean animOnly) {
        return new FilterState(this, animOnly);
    }

    public FilterState createPreset(String name) {
        FilterState preset = copyState(false);
        preset.setName(name);
        return preset;
    }

    public void setState(FilterState newStateSet, boolean forAnimation) {
        for (FilterParam param : paramList) {
            if (forAnimation && !param.canBeAnimated()) {
                continue;
            }
            String name = param.getName();
            ParamState<?> newState = newStateSet.get(name);

            if (newState != null) { // a preset doesn't have to contain all key-value pairs
                param.setState(newState, !forAnimation);
            }
        }
    }

    public void applyPreset(FilterState preset) {
        setState(preset, false);
        runFilter();
    }

    public void loadPreset(UserPreset preset) {
        long runCountBefore = Filter.runCount;
        System.out.println("ParamSet::loadPreset: loading from preset " + preset.toString());
        for (FilterParam param : paramList) {
            param.loadStateFrom(preset);
        }
        assert runCountBefore == Filter.runCount :
            "runCountBefore = " + runCountBefore + ", runCount = " + Filter.runCount;

        runFilter();

        assert runCountBefore + 1 == Filter.runCount :
            "runCountBefore = " + runCountBefore + ", runCount = " + Filter.runCount;
    }

    /**
     * A ParamSet can be animated if at least
     * one contained filter parameter can be
     */
    public boolean canBeAnimated() {
        return Utils.anyMatch(paramList, FilterParam::canBeAnimated);
    }

    public void setFinalAnimationSettingMode(boolean b) {
        for (FilterParam param : paramList) {
            param.setEnabled(!b, FINAL_ANIMATION_SETTING);
        }
        for (FilterButtonModel action : actionList) {
            action.setEnabled(!b, FINAL_ANIMATION_SETTING);
        }
    }

    public boolean hasGradient() {
        return Utils.anyMatch(paramList, p -> p instanceof GradientParam);
    }

    public List<FilterButtonModel> getActions() {
        return actionList;
    }

    public List<FilterParam> getParams() {
        return paramList;
    }

    public void setBuiltinPresets(FilterState... filterStates) {
        this.builtinPresets = filterStates;
    }

    public FilterState[] getBuiltinPresets() {
        return builtinPresets;
    }

    public boolean hasBuiltinPresets() {
        return builtinPresets != null;
    }

    public UserPreset toUserPreset(String filterName, String presetName) {
        UserPreset p = new UserPreset(presetName, filterName);
        for (FilterParam param : paramList) {
            param.saveStateTo(p);
        }
        return p;
    }

    public boolean isNonTrivialFilter() {
        return nonTrivialFilter;
    }

    @Override
    public String toString() {
        return "ParamSet {" + paramList.toString() + "}";
    }
}
