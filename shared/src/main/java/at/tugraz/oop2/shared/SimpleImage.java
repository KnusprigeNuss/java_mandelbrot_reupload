package at.tugraz.oop2.shared;

import at.tugraz.oop2.shared.exception.InvalidDepthException;
import javafx.scene.paint.Color;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

public class SimpleImage implements Serializable {
    @Getter
    private short[] data;
    @Getter
    private int depth;
    @Getter
    private int width;
    @Getter
    private int height;

    public SimpleImage(int width, int height) {
        this(3, width, height);
    }

    @SuppressWarnings("unused")
    public SimpleImage(List<SimpleImage> interleaved) throws Exception {
        this(3, 0, 0);
        if (interleaved.size() > 0) {
            this.width = interleaved.get(0).getWidth();
            this.height = interleaved.stream().mapToInt(SimpleImage::getHeight).sum();
            this.depth = interleaved.get(0).getDepth();
            this.data = new short[width * height * depth];
            for (int i = 0; i < interleaved.size(); i++) {
                SimpleImage subImage = interleaved.get(i);

                for (int j = 0; j < subImage.height; j++) {
                    for (int k = 0; k < subImage.width; k++) {
                        int y = i + interleaved.size() * j;
                        if (y < height) {
                            setPixel(k, y, subImage.getPixel(k, j));
                        }
                    }
                }
            }

        }
    }

    @SuppressWarnings("unused")
    public SimpleImage(List<SimpleImage> interleaved, boolean a) throws Exception {
        this(3, 0, 0);
        if (interleaved.size() > 0) {
            this.width = interleaved.get(0).getWidth();
            this.height = interleaved.stream().mapToInt(SimpleImage::getHeight).sum();
            this.depth = interleaved.get(0).getDepth();
            this.data = new short[width * height * depth];
            int current_height = 0;
            for (int i = 0; i < interleaved.size(); i++) {
                SimpleImage subImage = interleaved.get(i);
                if(i != 0){
                    current_height += interleaved.get(i-1).getHeight();
                }
                for (int j = 0; j < subImage.height; j++) {
                    for (int k = 0; k < subImage.width; k++) {
                        int y = current_height + j;
                        if (y < height) {
                            setPixel(k, y, subImage.getPixel(k, j));
                        }
                    }
                }
            }
        }
    }

    public SimpleImage(int depth, int width, int height) {
        this.depth = depth;
        this.width = width;
        this.height = height;
        this.data = new short[width * height * depth];
    }

    @SuppressWarnings("unused")
    public byte[] getByteData() {
        byte[] arr = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            arr[i] = (byte) data[i];
        }
        return arr;
    }

    public void setPixel(int x, int y, short[] data) throws InvalidDepthException {
        if (data.length != depth) {
            throw new InvalidDepthException();
        }
        for (int i = 0; i < depth; i++) {
            this.data[width * y * depth + x * depth + i] = data[i];
        }
    }

    public short[] getPixel(int x, int y) {
        short[] pixel = new short[depth];
        for (int i = 0; i < depth; i++) {
            try { pixel[i] = this.data[width * y * depth + x * depth + i]; }
            catch (Exception e) {
                // empty
            }
        }
        return pixel;
    }

    double[] colorGreg(String colour){
        String hex = colour.replace("0x", "#");
        Color c = Color.web(hex);
        return (new double[] {c.getRed()*255, c.getGreen()*255, c.getBlue()*255});
    }


    public short[] colorOptions(ColourModes mode, boolean not_part_of_set, int it_exit, int it){
        // colors are set
        short[] data = new short[depth];
        // black and white mode
        // only important if pixel is part of set
        if(mode == ColourModes.BLACK_WHITE) {
            if (not_part_of_set){
                for(int i = 0; i < depth; i++){
                    data[i] = 255; // white
                }
            }
            else{
                for(int i = 0; i < depth; i++){
                    data[i] = 0; // black
                }
            }
            return data;
        }
        // color mode
        // iteration count important for this
        else if(mode == ColourModes.COLOUR_FADE || mode == ColourModes.CUSTOM || mode == ColourModes.DISCO_MODE){
            if(not_part_of_set){
                double t = it_exit/((double)(it));
                double[] blue = {0, 0, 255};
                double[] red = {255, 0, 0};
                double[] color1 = blue; // any RGB Color can be added
                double[] color2 = red; // any RBG Color can be added
                if(mode == ColourModes.CUSTOM || mode == ColourModes.DISCO_MODE){
                    String colour1 = Colours.getColour1();
                    color2 = colorGreg(colour1);
                    String colour2 = Colours.getColour2();
                    color1 = colorGreg(colour2);
                }
                for(int i = 0; i < depth; i++) {
                    data[i] = (short) (color2[i] * t + color1[i] * (1 - t));
                }
            }
            else{ // black if not part of set
                for(int i = 0; i < depth; i++){
                    data[i] = 0;
                }
            }
            return data;
        }

        return data;
    }

    public void mandelbrotCalculation(MandelbrotRenderOptions renderOptions, int y_start)
    {
        int iterations = renderOptions.getIterations();
        double zoom = renderOptions.getZoom();
        double k = renderOptions.getPower();
        double mandelbrot_x = renderOptions.getCenterX();
        double mandelbrot_y = renderOptions.getCenterY();

        int w_screen = renderOptions.getWidth();
        int w_segment = this.width;
        int h_screen = renderOptions.getHeight();
        int h_segment = this.height;

        double w_complex = Math.pow(2,(2 - zoom));
        double h_complex = (h_screen/(double)w_screen) * w_complex;


        for(int x_screen = 0; x_screen < w_segment; x_screen++ ){
            for(int y_screen = 0; y_screen < h_segment; y_screen++ ){
                double x_complex = (x_screen) * (w_complex/(w_screen - 1.d)) - (w_complex/2.d) + mandelbrot_x;
                double y_complex = (y_screen + y_start) * (h_complex/(h_screen - 1.d)) - (h_complex/2.d) + mandelbrot_y;
                ImaginaryNumber c = new ImaginaryNumber(x_complex, y_complex);
                ImaginaryNumber z = new ImaginaryNumber(0,0);

                int it_count = 0;
                boolean not_part_of_set = false;

                while(it_count < iterations){
                    z.powToK(k);
                    z.add(c);
                    if(z.magnitude_squared() < 4) { // more performance as it does not have to be sqrt-ed every time
                        it_count++;
                    }
                    else {
                        not_part_of_set = true;
                        break;
                    }
                }

                // data contains the RGB values of the pixel at x_screen, y_screen
                short[] data = colorOptions(renderOptions.getMode(), not_part_of_set, it_count, iterations);

                try {
                    setPixel(x_screen, y_screen, data);
                } catch (InvalidDepthException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    // changed, should work now
    public void juliaCalculation(JuliaRenderOptions renderOptions, int y_start){
        if (renderOptions.getType() == FractalType.BURNING_SHIP
                || renderOptions.getType() == FractalType.BURNING_BIRD){
            burningShipCalculation(renderOptions,y_start);
            return;
        }
        else if (renderOptions.getType() == FractalType.BURNING_JULIA
                || renderOptions.getType() == FractalType.BURNING_JULIA_BIRD) {
            burningJuliaCalculation(renderOptions,y_start);
            return;
        }
        else if (renderOptions.getType() == FractalType.NEWTON_POLY
                || renderOptions.getType() == FractalType.NEWTON_SINE){
            newton(renderOptions,y_start);
            return;
        }

        int iterations = renderOptions.getIterations();
        double zoom = renderOptions.getZoom();
        double k = renderOptions.getPower();
        double julia_x = renderOptions.getCenterX();
        double julia_y = renderOptions.getCenterY();

        double w_screen = renderOptions.getWidth();
        int w_segment = this.width;
        double h_screen = renderOptions.getHeight();
        int h_segment = this.height;

        double w_complex = Math.pow(2,(2-zoom));
        double h_complex = (w_complex * h_screen)/w_screen;

        ImaginaryNumber c = new ImaginaryNumber(renderOptions.getConstantX(), renderOptions.getConstantY());

        for(int x_screen = 0; x_screen < w_segment; x_screen++ ){
            for(int y_screen = 0; y_screen < h_segment; y_screen++ ){
                double x_complex = (x_screen * (w_complex/(w_screen - 1.d))) - (w_complex/2.d) + julia_x;
                double y_complex = (y_screen + y_start) * (h_complex/(h_screen - 1.d)) - h_complex/2.d + julia_y;

                ImaginaryNumber z = new ImaginaryNumber(x_complex, y_complex);

                int it_count = 0;
                boolean not_part_of_set = false;

                while(it_count < iterations){
                    z.powToK(k);
                    z.add(c);
                    if(z.magnitude_squared() < 4) {
                        it_count++;
                    }
                    else {
                        not_part_of_set = true;
                        break;
                    }
                }

                if(iterations == 0){
                    if(z.magnitude_squared() >= 4) {
                        not_part_of_set = true;
                    }
                }

                // data contains the RGB values of the pixel at x_screen, y_screen
                short[] data = colorOptions(renderOptions.getMode(), not_part_of_set, it_count, iterations);

                try {
                    this.setPixel(x_screen, y_screen, data);
                } catch (InvalidDepthException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void burningShipCalculation(JuliaRenderOptions renderOptions, int y_start){
        int iterations = renderOptions.getIterations();
        double zoom = renderOptions.getZoom();
        double k = renderOptions.getPower();
        double julia_x = renderOptions.getCenterX();
        double julia_y = renderOptions.getCenterY();

        int w_screen = renderOptions.getWidth();
        int w_segment = this.width;
        int h_screen = renderOptions.getHeight();
        int h_segment = this.height;

        double w_complex = Math.pow(2,2-zoom);
        double h_complex = (h_screen/(double)w_screen) * w_complex;

        for(int x_screen = 0; x_screen < w_segment; x_screen++ ){
            for(int y_screen = 0; y_screen < h_segment; y_screen++ ){
                double x_complex = x_screen * w_complex/(w_screen - 1) - w_complex/2 + julia_x;
                double y_complex = (y_screen + y_start) * h_complex/(h_screen - 1) - h_complex/2 + julia_y;
                ImaginaryNumber c = new ImaginaryNumber(x_complex,y_complex);
                ImaginaryNumber z = new ImaginaryNumber(0,0);

                boolean not_part_of_set = true;
                int it_count = 0;
                while (z.magnitude_squared() < 4){
                    ImaginaryNumber absz = new ImaginaryNumber(Math.abs(z.getRealpart()), Math.abs(z.getImaginarypart()));
                    if(renderOptions.getType() == FractalType.BURNING_BIRD){
                        absz = new ImaginaryNumber(z.getRealpart(), -1*(Math.abs(z.getImaginarypart())));
                    }
                    absz.powToK(k);
                    z.copyValues(absz);
                    z.add(c);
                    if(it_count >= iterations - 1) {
                        it_count++;
                        break;
                    }
                    else {
                        it_count++;
                    }
                }
                if (z.magnitude_squared() < 4){
                    not_part_of_set = false; // part of set
                }
                if(it_count > 0) it_count--;

                // data contains the RGB values of the pixel at x_screen, y_screen
                short[] data = colorOptions(renderOptions.getMode(), not_part_of_set, it_count, iterations);

                try {
                    this.setPixel(x_screen, y_screen, data);
                } catch (InvalidDepthException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
    public void burningJuliaCalculation(JuliaRenderOptions renderOptions, int y_start){
        int iterations = renderOptions.getIterations();
        double zoom = renderOptions.getZoom();
        double k = renderOptions.getPower();
        double julia_x = renderOptions.getCenterX();
        double julia_y = renderOptions.getCenterY();

        int w_screen = renderOptions.getWidth();
        int w_segment = this.width;
        int h_screen = renderOptions.getHeight();
        int h_segment = this.height;

        double w_complex = Math.pow(2,2-zoom);
        double h_complex = (h_screen/(double)w_screen) * w_complex;

        ImaginaryNumber c = new ImaginaryNumber(renderOptions.getConstantX(),renderOptions.getConstantY());

        for(int x_screen = 0; x_screen < w_segment; x_screen++ ){
            for(int y_screen = 0; y_screen < h_segment; y_screen++ ){
                double x_complex = x_screen * w_complex/(w_screen - 1) - w_complex/2 + julia_x;
                double y_complex = (y_screen + y_start) * h_complex/(h_screen - 1) - h_complex/2 + julia_y;

                ImaginaryNumber z = new ImaginaryNumber(x_complex,y_complex);

                boolean not_part_of_set = true;
                int it_count = 0;
                while (z.magnitude_squared() < 4){
                    ImaginaryNumber absz = new ImaginaryNumber(Math.abs(z.getRealpart()), Math.abs(z.getImaginarypart()));
                    if(renderOptions.getType() == FractalType.BURNING_JULIA_BIRD){
                        absz = new ImaginaryNumber(z.getRealpart(), -1*(Math.abs(z.getImaginarypart())));
                    }
                    absz.powToK(k);
                    z.copyValues(absz);
                    z.add(c);
                    if(it_count >= iterations - 1) {
                        it_count++;
                        break;
                    }
                    else {
                        it_count++;
                    }
                }
                if (z.magnitude_squared() < 4){
                    not_part_of_set = false; // part of set
                }
                if(it_count > 0) it_count--;

                // data contains the RGB values of the pixel at x_screen, y_screen
                short[] data = colorOptions(renderOptions.getMode(), not_part_of_set, it_count, iterations);

                try {
                    this.setPixel(x_screen, y_screen, data);
                } catch (InvalidDepthException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void newton(JuliaRenderOptions renderOptions, int y_start){
        int iterations = renderOptions.getIterations();
        double zoom = renderOptions.getZoom();
        double k = renderOptions.getPower();
        double julia_x = renderOptions.getCenterX();
        double julia_y = renderOptions.getCenterY();

        int w_screen = renderOptions.getWidth();
        int w_segment = this.width;
        int h_screen = renderOptions.getHeight();
        int h_segment = this.height;

        double w_complex = Math.pow(2,2-zoom);
        double h_complex = (h_screen/(double)w_screen) * w_complex;

        double compare = 2;

        if(renderOptions.getType() == FractalType.NEWTON_SINE){
            compare = renderOptions.power;
        }

        for(int x_screen = 0; x_screen < w_segment; x_screen++ ){
            for(int y_screen = 0; y_screen < h_segment; y_screen++ ){
                double x_complex = x_screen * w_complex/(w_screen - 1) - w_complex/2 + julia_x;
                double y_complex = (y_screen + y_start) * h_complex/(h_screen - 1) - h_complex/2 + julia_y;
                ImaginaryNumber z = new ImaginaryNumber(x_complex,y_complex);
                ImaginaryNumber a = new ImaginaryNumber(renderOptions.getConstantX(), renderOptions.getConstantY());

                boolean not_part_of_set = true;
                int it_count = 0;
                while (z.magnitude() < compare){
                    // function = z^power + z^power-3
                    // function derivate = (power - 1) * z + (power - 4) * z
                    if (renderOptions.getType() == FractalType.NEWTON_POLY){
                        ImaginaryNumber calc = (z.powToK(k, true));
                        calc.add(z.powToK(k - 3, true), true);
                        ImaginaryNumber calc2 = z.multiplication(k - 1);
                        ImaginaryNumber calc3 = z.multiplication(k - 4);
                        calc2.add(calc3);
                        ImaginaryNumber b = calc.division(calc2);
                        a = a.multiplication(b);
                        a = new ImaginaryNumber(-1 * a.getRealpart(), -1 * a.getImaginarypart());
                        z.add(a);
                    }

                    else {
                        z = z.sine();
                        z.add(a);
                    }

                    if(it_count >= iterations - 1) {
                        it_count++;
                        break;
                    }
                    else {
                        it_count++;
                    }
                }
                if (z.magnitude() < compare){
                    not_part_of_set = false; // part of set
                }
                if(it_count > 0) it_count--;

                // data contains the RGB values of the pixel at x_screen, y_screen
                short[] data = colorOptions(renderOptions.getMode(), not_part_of_set, it_count, iterations);

                try {
                    this.setPixel(x_screen, y_screen, data);
                } catch (InvalidDepthException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
