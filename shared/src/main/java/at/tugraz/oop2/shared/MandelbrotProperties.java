package at.tugraz.oop2.shared;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MandelbrotProperties extends FractalProperties {

    public MandelbrotProperties() {}

    public MandelbrotProperties(double centerX, double centerY, int width, int height, double zoom, double power, int iterations, ColourModes mode, int fragmentNumber, int totalFragments, RenderMode renderMode) {
        //super(centerX, centerY, width, height, zoom, power, iterations, FractalType.MANDELBROT, mode, 0, totalFragments, fragmentNumber, renderMode);
        this.centerX.set(centerX);
        this.centerY.set(centerY);
        this.width.set(width);
        this.height.set(height);
        this.zoom.set(zoom);
        this.power.set(power);
        this.iterations.set(iterations);
        this.requestId.set(0);
        this.totalFragments.set(totalFragments);
        this.fragmentNumber.set(fragmentNumber);

        this.type = FractalType.MANDELBROT;
        this.mode.setSelectedMode(mode);
        this.renderMode.setSelectedMode(renderMode);
    }

    public MandelbrotProperties(double centerX, double centerY, int width, int height, double zoom, double power, int iterations, ColourModes mode, RenderMode renderMode) {
        this(centerX, centerY, width, height, zoom, power, iterations, mode, 0, 1, renderMode);
    }

}
