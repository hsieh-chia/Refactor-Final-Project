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

package pixelitor.menus;

import com.jhlabs.image.AbstractBufferedImageOp;
import pixelitor.filters.Filter;
import pixelitor.filters.util.FilterAction;

import javax.swing.*;
import java.util.function.Supplier;

/**
 * A JMenu with some utility methods
 */
public class PMenu extends JMenu {
    public PMenu(String s) {
        super(s);
    }

    public PMenu(String s, char c) {
        super(s);
        setMnemonic(c);
    }

    // Simple add without a builder
    public void addAction(Action action) {
        JMenuItem menuItem = EnabledIf.THERE_IS_OPEN_IMAGE.createMenuItem(action);
        add(menuItem);
    }

    // Simple add without a builder
    public void addActionWithKey(Action action, KeyStroke keyStroke) {
        JMenuItem menuItem = EnabledIf.THERE_IS_OPEN_IMAGE.createMenuItem(action);
        menuItem.setAccelerator(keyStroke);
        add(menuItem);
    }

    // Simple add without a builder
    public void addAlwaysEnabledAction(Action action, KeyStroke keyStroke) {
        JMenuItem menuItem = EnabledIf.ACTION_ENABLED.createMenuItem(action);
        menuItem.setAccelerator(keyStroke);
        add(menuItem);
    }

    /**
     * Returns an action builder for non-filter actions
     */
    public MenuItemBuilder buildAction(Action action) {
        return new MenuItemBuilder(this, action);
    }

    /**
     * Simple add for filter actions, no builder is needed in the simplest case
     */
    public void addFilter(String name, Supplier<Filter> supplier) {
        addFilter(new FilterAction(name, supplier));
    }

    /**
     * Simple add for simple filters
     */
    public void addFilter(String name, AbstractBufferedImageOp op) {
        addFilter(new FilterAction(name, op));
    }

    public void addFilter(FilterAction fa) {
        JMenuItem menuItem = EnabledIf.THERE_IS_OPEN_IMAGE.createMenuItem(fa);
        add(menuItem);
    }

    public FilterMenuItemBuilder buildFilter(String name, Supplier<Filter> supplier) {
        FilterAction fa = new FilterAction(name, supplier);
        return buildFilter(fa);
    }

    public FilterMenuItemBuilder buildFilter(FilterAction fa) {
        return new FilterMenuItemBuilder(this, fa);
    }

    /**
     * Action builder for non-filter actions
     */
    public static class MenuItemBuilder {
        private final PMenu menu;
        protected final Action action;
        private KeyStroke keyStroke;
        private EnabledIf whenToEnable;

        public MenuItemBuilder(PMenu menu, Action action) {
            this.action = action;
            this.menu = menu;
        }

        public void add() {
            if (whenToEnable == null) {
                whenToEnable = EnabledIf.THERE_IS_OPEN_IMAGE;
            }
            JMenuItem menuItem = whenToEnable.createMenuItem(action);
            menu.add(menuItem);
            if (keyStroke != null) {
                menuItem.setAccelerator(keyStroke);
            }
        }

        public MenuItemBuilder withKey(KeyStroke keyStroke) {
            this.keyStroke = keyStroke;
            return this;
        }

        public MenuItemBuilder enableIf(EnabledIf whenToEnable) {
            this.whenToEnable = whenToEnable;
            return this;
        }

        public MenuItemBuilder alwaysEnabled() {
            // used in cases when the action will be never disabled
            whenToEnable = EnabledIf.ACTION_ENABLED;
            return this;
        }
    }

    /**
     * Filter action builder
     */
    public static class FilterMenuItemBuilder extends MenuItemBuilder {
        public FilterMenuItemBuilder(PMenu menu, FilterAction action) {
            super(menu, action);
        }

        public FilterMenuItemBuilder noGUI() {
            ((FilterAction) action).withoutGUI();
            return this;
        }

        public FilterMenuItemBuilder withFillListName() {
            ((FilterAction) action).withFillListName();
            return this;
        }

        public FilterMenuItemBuilder extract() {
            ((FilterAction) action).withExtractChannelListName();
            return this;
        }
    }
}
