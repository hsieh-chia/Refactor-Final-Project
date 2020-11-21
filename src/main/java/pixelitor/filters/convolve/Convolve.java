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

package pixelitor.filters.convolve;

import org.jdesktop.swingx.combobox.EnumComboBoxModel;
import pixelitor.filters.gui.FilterGUI;
import pixelitor.filters.gui.FilterWithGUI;
import pixelitor.filters.util.FilterAction;
import pixelitor.layers.Drawable;
import pixelitor.utils.Messages;

import java.awt.image.BufferedImage;
import java.awt.image.ImagingOpException;
import java.awt.image.Kernel;
import java.util.Random;

/**
 * A customizable convolution
 */
public class Convolve extends FilterWithGUI {
    private final String filterName;

    private final EnumComboBoxModel<ConvolveMethod> convolveMethodModel = new EnumComboBoxModel<>(ConvolveMethod.class);

    private float[] kernelMatrix;
    private final int size;

    private Convolve(int size, String filterName) {
        this.size = size;
        this.filterName = filterName;
    }

    public void setKernelMatrix(float[] kernelMatrix) {
        if (kernelMatrix.length != size * size) {
            throw new IllegalArgumentException("kernelMatrix.length = " + kernelMatrix.length + ", size = " + size);
        }
        this.kernelMatrix = kernelMatrix;
    }

    @Override
    public BufferedImage transform(BufferedImage src, BufferedImage dest) {
        var kernel = new Kernel(size, size, kernelMatrix);
        var convolveMethod = convolveMethodModel.getSelectedItem();
        var convolveOp = convolveMethod.createConvolveOp(kernel, filterName);
        try {
            convolveOp.filter(src, dest);
        } catch (ImagingOpException e) {
            Messages.showException(e);
        }

        return dest;
    }

    @Override
    public FilterGUI createGUI(Drawable dr) {
        return new CustomConvolveGUI(this, dr);
    }

    public EnumComboBoxModel<ConvolveMethod> getConvolveMethodModel() {
        return convolveMethodModel;
    }

    @Override
    public void randomizeSettings() {
        kernelMatrix = getRandomKernelMatrix(size);
    }

    /**
     * Returns a randomized array that is on average close to being normalized
     */
    public static float[] getRandomKernelMatrix(int size) {
        Random rand = new Random();
        float[] retVal = new float[size * size];
        for (int i = 0; i < retVal.length; i++) {
            int randomInt = rand.nextInt(10000);
            retVal[i] = (4 * randomInt / (10000.0f * retVal.length)) - (1.0f / retVal.length);
        }

        return retVal;
    }

    public int getSize() {
        return size;
    }

    public static FilterAction createFilterAction(int size) {
        String name = getFilterName(size, size);
        return new FilterAction(name, () -> new Convolve(size, name));
    }

    private static String getFilterName(int width, int height) {
        return "Custom " + width + 'x' + height + " Convolution";
    }
}
