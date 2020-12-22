public class convolveH implements IConvolve{
    @Override
    public void convolveProcessing(Kernel kernel, int[] inputPixels, int[] outputPixels, int width, int height, boolean alpha, int edgeAction) {
        int index = 0;
        float[] matrix = kernel.getKernelData(null);
        int cols = kernel.getWidth();
        int halfCols = cols / 2;

        for (int y = 0; y < height; y++) {
            int ioffset = y * width;
            for (int x = 0; x < width; x++) {
                float r = 0, g = 0, b = 0, a = 0;
                int moffset = halfCols;
                for (int col = -halfCols; col <= halfCols; col++) {
                    float f = matrix[moffset + col];

                    if (f != 0) {
                        int eachRoundX = x + col;
                        if (eachRoundX < 0) {
                            if (edgeAction == CLAMP_EDGES) {
                                eachRoundX = 0;
                            } else if (edgeAction == WRAP_EDGES) {
                                eachRoundX = (x + width) % width;
                            }
                        } else if (eachRoundX >= width) {
                            if (edgeAction == CLAMP_EDGES) {
                                eachRoundX = width - 1;
                            } else if (edgeAction == WRAP_EDGES) {
                                eachRoundX = (x + width) % width;
                            }
                        }
                        int rgb = inputPixels[ioffset + eachRoundX];
                        a += f * ((rgb >> 24) & 0xff);
                        r += f * ((rgb >> 16) & 0xff);
                        g += f * ((rgb >> 8) & 0xff);
                        b += f * (rgb & 0xff);
                    }
                }
                int eachRoundAlpha = alpha ? PixelUtils.clamp((int) (a + 0.5)) : 0xff;
                int eachRoundRed = PixelUtils.clamp((int) (r + 0.5));
                int eachRoundGreen = PixelUtils.clamp((int) (g + 0.5));
                int eachRoundBlue = PixelUtils.clamp((int) (b + 0.5));
                outputPixels[index++] = (eachRoundAlpha << 24) | (eachRoundRed << 16) | (eachRoundGreen << 8) | eachRoundBlue;
            }
        }
    }
}