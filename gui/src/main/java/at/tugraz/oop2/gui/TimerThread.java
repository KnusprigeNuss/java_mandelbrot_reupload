package at.tugraz.oop2.gui;
import java.util.Date;
import at.tugraz.oop2.shared.*;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.util.Random;

public class TimerThread extends Thread {

    public static JuliaRenderOptions julia;
    public JuliaRenderOptions julia_cmp;
    public static MandelbrotRenderOptions mandel;
    public MandelbrotRenderOptions mandel_cmp;
    static final IntegerProperty requested_new_image = new SimpleIntegerProperty();
    public static long start_render_time = 0;

    //remove later
    public static long x = 0;

    public static SimpleBooleanProperty panning_activated = new SimpleBooleanProperty(false);
    public static SimpleBooleanProperty zoom_activated = new SimpleBooleanProperty(false);

    @SuppressWarnings("all")
    public void run(){

        while(true) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            synchronized (requested_new_image) {
                if (FractalApplication.julia_instance.getMode() == ColourModes.DISCO_MODE) {
                    requested_new_image.set(3);
                }
                if(requested_new_image.get() == 0) {
                    continue;
                }
            }

            synchronized (FractalApplication.julia_instance) {
                //System.out.println("Property class: " + FractalApplication.julia_instance.getType());
                julia = new JuliaRenderOptions(FractalApplication.julia_instance.getCenterX(), FractalApplication.julia_instance.getCenterY(),FractalApplication.julia_instance.getWidth(),FractalApplication.julia_instance.getHeight(),FractalApplication.julia_instance.getZoom(),FractalApplication.julia_instance.getPower(),FractalApplication.julia_instance.getIterations(),FractalApplication.julia_instance.getConstantX(),FractalApplication.julia_instance.getConstantY(),FractalApplication.julia_instance.getMode(),FractalApplication.julia_instance.getFragmentNumber(),FractalApplication.julia_instance.getTotalFragments(),FractalApplication.julia_instance.getRenderMode(), FractalApplication.julia_instance.getType());
                //System.out.println("Property after greg: " + julia.getType());
            }
            synchronized (FractalApplication.mandel_instance) {
                mandel = new MandelbrotRenderOptions(FractalApplication.mandel_instance.getCenterX(),FractalApplication.mandel_instance.getCenterY(),FractalApplication.mandel_instance.getWidth(),FractalApplication.mandel_instance.getHeight(),FractalApplication.mandel_instance.getZoom(),FractalApplication.mandel_instance.getPower(),FractalApplication.mandel_instance.getIterations(),FractalApplication.mandel_instance.getMode(),FractalApplication.mandel_instance.getRenderMode());
            }


            boolean wait = true;
            try {
                Thread.sleep(18);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            synchronized (zoom_activated){
                 if(zoom_activated.getValue() == false){
                    wait = false;
                 }
                 else {
                    zoom_activated.set(false);
                 }
            }

            synchronized (panning_activated){
                if(panning_activated.getValue() == false){
                    wait = true;
                }
            }
            if(wait == true){
                try {
                    Thread.sleep(282);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }


            synchronized (FractalApplication.julia_instance) {
                julia_cmp = new JuliaRenderOptions(FractalApplication.julia_instance.getCenterX(), FractalApplication.julia_instance.getCenterY(),FractalApplication.julia_instance.getWidth(),FractalApplication.julia_instance.getHeight(),FractalApplication.julia_instance.getZoom(),FractalApplication.julia_instance.getPower(),FractalApplication.julia_instance.getIterations(),FractalApplication.julia_instance.getConstantX(),FractalApplication.julia_instance.getConstantY(),FractalApplication.julia_instance.getMode(),FractalApplication.julia_instance.getFragmentNumber(),FractalApplication.julia_instance.getTotalFragments(),FractalApplication.julia_instance.getRenderMode(), FractalApplication.julia_instance.getType());
            }
            synchronized (FractalApplication.mandel_instance) {
                mandel_cmp = new MandelbrotRenderOptions(FractalApplication.mandel_instance.getCenterX(),FractalApplication.mandel_instance.getCenterY(),FractalApplication.mandel_instance.getWidth(),FractalApplication.mandel_instance.getHeight(),FractalApplication.mandel_instance.getZoom(),FractalApplication.mandel_instance.getPower(),FractalApplication.mandel_instance.getIterations(),FractalApplication.mandel_instance.getMode(),FractalApplication.mandel_instance.getRenderMode());
            }

            if (julia.equals(julia_cmp) && mandel.equals(mandel_cmp)) {
                synchronized (requested_new_image) {
                    if (julia.getMode() == ColourModes.DISCO_MODE) {
                        Random random_obj = new Random();
                        int random_hex = random_obj.nextInt(0xffffff + 1);
                        String new_colour = String.format("#%06x", random_hex);
                        Colours.colour1.set(new_colour);
                        random_hex = random_obj.nextInt(0xffffff + 1);
                        new_colour = String.format("#%06x", random_hex);
                        Colours.colour2.set(new_colour);
                    }
                    if (requested_new_image.get() == 1) {
                        Date date = new Date();
                        start_render_time = date.getTime();
                        FractalApplication.mandelRenderManger.triggerRendering(mandel);
                        FractalLogger.logRenderCallGUI(mandel);
                    } else if (requested_new_image.get() == 2) {
                        Date date = new Date();
                        start_render_time = date.getTime();
                        FractalApplication.juliaRenderManger.triggerRendering(julia);
                        FractalLogger.logRenderCallGUI(julia);
                    } else {
                        Date date = new Date();
                        start_render_time = date.getTime();
                        System.out.println("start render time at beginning: " + start_render_time);
                        FractalApplication.mandelRenderManger.triggerRendering(mandel);
                        FractalApplication.juliaRenderManger.triggerRendering(julia);
                        FractalLogger.logRenderCallGUI(mandel);
                        FractalLogger.logRenderCallGUI(julia);
                    }
                    //FractalApplication.renderProgressBar.setProgress(FractalApplication.renderProgressBar.getProgress() + 0.1);
                    requested_new_image.set(0);
                }
            }
        }
    }

    public JuliaRenderOptions getCurrentJulia()
    {
        JuliaRenderOptions temp;
        synchronized (FractalApplication.julia_instance) {
            temp = new JuliaRenderOptions(FractalApplication.julia_instance.getCenterX(), FractalApplication.julia_instance.getCenterY(),FractalApplication.julia_instance.getWidth(),FractalApplication.julia_instance.getHeight(),FractalApplication.julia_instance.getZoom(),FractalApplication.julia_instance.getPower(),FractalApplication.julia_instance.getIterations(),FractalApplication.julia_instance.getConstantX(),FractalApplication.julia_instance.getConstantY(),FractalApplication.julia_instance.getMode(),FractalApplication.julia_instance.getFragmentNumber(),FractalApplication.julia_instance.getTotalFragments(),FractalApplication.julia_instance.getRenderMode(), FractalApplication.julia_instance.getType());
        }
        return temp;
    }

    public MandelbrotRenderOptions getCurrentMandel()
    {
        MandelbrotRenderOptions temp;
        synchronized (FractalApplication.mandel_instance) {
            temp = new MandelbrotRenderOptions(FractalApplication.mandel_instance.getCenterX(),FractalApplication.mandel_instance.getCenterY(),FractalApplication.mandel_instance.getWidth(),FractalApplication.mandel_instance.getHeight(),FractalApplication.mandel_instance.getZoom(),FractalApplication.mandel_instance.getPower(),FractalApplication.mandel_instance.getIterations(),FractalApplication.mandel_instance.getMode(),FractalApplication.mandel_instance.getRenderMode());
        }
        return temp;
    }

    public static void setRequestNewImage(int setter) {
        synchronized (requested_new_image) {
            if (requested_new_image.get() == 1 && (setter == 2 || setter == 3)) {
                requested_new_image.set(3);
            } else if (requested_new_image.get() == 2 && (setter == 1 || setter == 3)) {
                requested_new_image.set(3);
            } else if (requested_new_image.get() == 3) {
                requested_new_image.set(3);
            } else {
                requested_new_image.set(setter);
            }
        }
    }

    public static void finishedRenderCall() {
        //System.out.println("here");
        Date date = new Date();
        long end_render_time = date.getTime();
        //System.out.println("start render time in the end: " + start_render_time);
        //System.out.println("end render time: " + end_render_time);
        long total_render_time = end_render_time - start_render_time;
        //System.out.println(total_render_time);
        String s = total_render_time + " ms";
        Platform.runLater(() -> {FractalApplication.timestamp_label.setText(s);});
    }
}
