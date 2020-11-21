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

import pixelitor.layers.Layer;
import pixelitor.layers.LayerMask;

public class LayerNode extends DebugNode {
    public LayerNode(String name, Layer layer) {
        super(name, layer);

        if (layer.hasMask()) {
            LayerMask mask = layer.getMask();
            add(new LayerMaskNode(mask));
        } else {
            addString("has mask", "no");
        }

        addBoolean("mask enabled", layer.isMaskEnabled());
        addBoolean("mask editing", layer.isMaskEditing());
        addBoolean("visible", layer.isVisible());

        addFloat("opacity", layer.getOpacity());
        addQuotedString("blending mode", layer.getBlendingMode().toString());
        addQuotedString("name", layer.getName());
    }
}
