package at.tugraz.oop2.shared;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class JuliaProperties extends FractalProperties {
    //private double constantX;
    //private double constantY;
    private DoubleProperty constantX = new SimpleDoubleProperty();
    private DoubleProperty constantY = new SimpleDoubleProperty();

    public JuliaProperties() {}

    public JuliaProperties(double centerX, double centerY, int width, int height, double zoom, double power, int iterations, double constantX, double constantY, ColourModes mode, int fragmentNumber, int totalFragments, RenderMode renderMode, FractalType fractal_type) {
        //super(centerX, centerY, width, height, zoom, power, iterations, FractalType.JULIA, mode, 0, totalFragments, fragmentNumber, renderMode);
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

        this.type = fractal_type;
        this.mode.setSelectedMode(mode);
        this.renderMode.setSelectedMode(renderMode);


        this.constantX.set(constantX);
        this.constantY.set(constantY);
    }

    public JuliaProperties(double centerX, double centerY, int width, int height, double zoom, double power, int iterations, double constantX, double constantY, ColourModes mode, RenderMode renderMode, FractalType fractal_type) {
        this(centerX, centerY, width, height, zoom, power, iterations, constantX, constantY, mode, 0, 1, renderMode, fractal_type);
    }

    public double getConstantX() {
        return constantX.get();
    }

    public DoubleProperty constantXProperty() {
        return constantX;
    }

    public void setConstantX(double constantX) {
        this.constantX.set(constantX);
    }

    public double getConstantY() {
        return constantY.get();
    }

    public DoubleProperty constantYProperty() {
        return constantY;
    }

    public void setConstantY(double constantY) {
        this.constantY.set(constantY);
    }
}
