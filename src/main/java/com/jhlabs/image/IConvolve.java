public interface IConvolve {
    public void convolveProcessing(Kernel kernel, int[] inputPixels, int[] outputPixels, int width, int height, boolean alpha, int edgeAction);
}