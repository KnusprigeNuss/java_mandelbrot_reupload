package at.tugraz.oop2.shared;

import javafx.beans.property.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Getter;


@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Getter
public abstract class FractalProperties {
    //protected double centerX;
    protected DoubleProperty centerX = new SimpleDoubleProperty();
    protected DoubleProperty centerY = new SimpleDoubleProperty();
    protected IntegerProperty width = new SimpleIntegerProperty();
    protected IntegerProperty height = new SimpleIntegerProperty();
    protected DoubleProperty zoom = new SimpleDoubleProperty();
    protected DoubleProperty power = new SimpleDoubleProperty();
    protected IntegerProperty iterations = new SimpleIntegerProperty();
    protected FractalType type;
    protected ColorModeProperty mode = new ColorModeProperty();
    protected LongProperty requestId = new SimpleLongProperty();
    protected IntegerProperty totalFragments = new SimpleIntegerProperty();
    protected IntegerProperty fragmentNumber = new SimpleIntegerProperty();
    protected RenderModeProperty renderMode = new RenderModeProperty();

    public double getCenterX() {
        return centerX.get();
    }

    public DoubleProperty centerXProperty() {
        return centerX;
    }

    public void setCenterX(double centerX) {
        this.centerX.set(centerX);
    }

    public double getCenterY() {
        return centerY.get();
    }

    public DoubleProperty centerYProperty() {
        return centerY;
    }

    public void setCenterY(double centerY) {
        this.centerY.set(centerY);
    }

    public int getWidth() {
        return width.get();
    }

    public void setWidth(int width) {
        this.width.set(width);
    }

    public int getHeight() {
        return height.get();
    }

    public void setHeight(int height) {
        this.height.set(height);
    }

    public double getZoom() {
        return zoom.get();
    }

    public DoubleProperty zoomProperty() {
        return zoom;
    }

    public void setZoom(double zoom) {
        this.zoom.set(zoom);
    }

    public double getPower() {
        return power.get();
    }

    public DoubleProperty powerProperty() {
        return power;
    }

    public void setPower(double power) {
        this.power.set(power);
    }

    public int getIterations() {
        return iterations.get();
    }

    public IntegerProperty iterationsProperty() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations.set(iterations);
    }

    public FractalType getType() {
        return type;
    }

    public void setType(FractalType type) {
        this.type = type;
    }

    public ColourModes getMode() {
        return this.mode.getSelectedMode();
    }

    public void setMode(ColourModes mode) {
        this.mode.setSelectedMode(mode);
    }

    public long getRequestId() {
        return requestId.get();
    }

    public LongProperty requestIdProperty() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId.set(requestId);
    }

    public int getTotalFragments() {
        return totalFragments.get();
    }

    public IntegerProperty totalFragmentsProperty() {
        return totalFragments;
    }

    public void setTotalFragments(int totalFragments) {
        this.totalFragments.set(totalFragments);
    }

    public int getFragmentNumber() {
        return fragmentNumber.get();
    }

    public IntegerProperty fragmentNumberProperty() {
        return fragmentNumber;
    }

    public void setFragmentNumber(int fragmentNumber) {
        this.fragmentNumber.set(fragmentNumber);
    }

    public RenderMode getRenderMode() {
        return renderMode.getSelectedMode();
    }

    public void setRenderMode(RenderMode renderMode) {
        this.renderMode.setSelectedMode(renderMode);
    }
}
