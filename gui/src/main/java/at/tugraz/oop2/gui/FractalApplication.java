package at.tugraz.oop2.gui;

import at.tugraz.oop2.shared.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.*;
import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import lombok.Getter;

// vs import
import javafx.scene.paint.Color;
import javafx.scene.image.PixelWriter;
//david includes
import javafx.scene.control.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.geometry.*;
import javafx.scene.input.ScrollEvent;

import java.beans.EventHandler;
import java.io.IOException;
import javafx.util.StringConverter;
import javafx.scene.input.MouseEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//david includes end

//alex imports
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FractalApplication extends Application {

    private static GridPane mainPane;
    private static Canvas rightCanvas;
    private static Canvas leftCanvas;
    private static GridPane controlPane;
    public static final JuliaProperties julia_instance = new JuliaProperties();
    public static final MandelbrotProperties mandel_instance = new MandelbrotProperties();
    private TimerThread time_thread = new TimerThread();
    public static RenderManager mandelRenderManger = new RenderManager();
    public static RenderManager juliaRenderManger = new RenderManager();
    public static ConnectionManager connectionManager = new ConnectionManager();

    public static IntegerProperty taskPerWorkerProperty = new SimpleIntegerProperty();

    public static ComboBox renderModeSelectionBox = null;
    public static ProgressBar renderProgressBar = null;
    public static Label timestamp_label = null;

    public static final String ip_address_regex = "(((((25[0-5])|(2[0-4][0-9]))|([0-1]?[0-9]{1,2}))\\.){3}((((25[0-5])|(2[0-4][0-9]))|([0-1]?[0-9]{1,2})))|(\\w+))";
    public static final String port_regex = "((6553[0-5]|655[0-2][0-9])|(65[0-4][0-9]{2}|6[0-4]{2}[0-9]{2})|[0-5]?[0-9]{1,4})";
    @Getter
    private DoubleProperty leftHeight = new SimpleDoubleProperty();
    @Getter
    private DoubleProperty leftWidth = new SimpleDoubleProperty();
    @Getter
    private DoubleProperty rightHeight = new SimpleDoubleProperty();
    @Getter
    private DoubleProperty rightWidth = new SimpleDoubleProperty();

    public static void fillCanvas(Canvas canvas, SimpleImage image){
        PixelWriter w = canvas.getGraphicsContext2D().getPixelWriter();
        for(int i = 0; i < image.getWidth(); i++){
            for(int j = 0; j < image.getHeight(); j++){
                short[] data = image.getPixel(i,j);
                Color c = Color.rgb(data[0], data[1], data[2]);
                w.setColor(i,j,c);
            }
        }
    }

    public static void fillCanvas(CanvasSide side, SimpleImage image){
        if(side == CanvasSide.LEFT){
            fillCanvas(leftCanvas, image);
            FractalLogger.logDrawDoneGUI(FractalType.MANDELBROT);
        } else if (side == CanvasSide.RIGHT) {
            fillCanvas(rightCanvas, image);
            FractalLogger.logDrawDoneGUI(julia_instance.getType());
        }
    }

    public static double[] panningDifference(FractalRenderOptions renderOptions, double[] oldpixel, double[] newpixel){
        double zoom = renderOptions.getZoom();
        double h_screen = renderOptions.getHeight();
        double w_screen = renderOptions.getWidth();
        double center_x = renderOptions.getCenterX();
        double center_y = renderOptions.getCenterY();

        double w_complex = Math.pow(2,(2.d - zoom));
        double h_complex = (h_screen/w_screen) * w_complex;

        double old_x = (oldpixel[0]) * (w_complex/(w_screen - 1.d)) - (w_complex/2.d) + center_x;
        double old_y = (oldpixel[1]) * (h_complex/(h_screen - 1.d)) - (h_complex/2.d) + center_y;

        double new_x = (newpixel[0]) * (w_complex/(w_screen - 1.d)) - (w_complex/2.d) + center_x;
        double new_y = (newpixel[1]) * (h_complex/(h_screen - 1.d)) - (h_complex/2.d) + center_y;

        double[] difference = {(new_x - old_x), (new_y - old_y)};
        return difference;
    }

    private synchronized void updateJuliaZoomAsynch(double delta) {
        Platform.runLater(()-> {
            if (delta > 0) {
                synchronized (FractalApplication.julia_instance) {
                    julia_instance.setZoom(julia_instance.getZoom() + 0.02);
                }
            }
            if (delta < 0) {
                synchronized (FractalApplication.julia_instance) {
                    julia_instance.setZoom(julia_instance.getZoom() - 0.02);
                }
            }
            // newRender(FractalType.MANDELBROT);
        });
    }

    private synchronized void updateMandelZoomAsynch(double delta) {
        Platform.runLater(() -> {
            if (delta > 0) {
                mandel_instance.setZoom(mandel_instance.getZoom() + 0.02);
            }
            if (delta < 0) {
                mandel_instance.setZoom(mandel_instance.getZoom() - 0.02);
            }
            // newRender(FractalType.MANDELBROT);
        });
    }

    private void updateSizes() {

        Bounds leftSize = mainPane.getCellBounds(0, 0);
        leftCanvas.widthProperty().set(leftSize.getWidth());
        leftCanvas.heightProperty().set(leftSize.getHeight());

        Bounds rightSize = mainPane.getCellBounds(1, 0);
        rightCanvas.widthProperty().set(rightSize.getWidth());
        rightCanvas.heightProperty().set(rightSize.getHeight());

        julia_instance.setWidth((int)rightSize.getWidth());
        julia_instance.setHeight((int)rightSize.getHeight());

        mandel_instance.setWidth((int)leftSize.getWidth());
        mandel_instance.setHeight((int)leftSize.getHeight());

        TimerThread.setRequestNewImage(3);

    }

    //checks if the given string is a number
    public boolean numericCheck(String teststr) {
        try {
            Double.parseDouble(teststr);
        } catch (NumberFormatException a) {
            return false;
        }
        return true;
    }

    //parses the arguments and puts the valid ones into a string array
    //otherwise uses the default values
    private ArrayList<String> parseParameters() throws IOException, InterruptedException {
        //compare strings
        String iterations_cmp = "iterations";
        String power_cmp = "power";
        String mandelbrotX_cmp = "mandelbrotx";
        String mandelbrotY_cmp = "mandelbroty";
        String mandelbrotZoom_cmp = "mandelbrotzoom";
        String juliaX_cmp = "juliax";
        String juliaY_cmp = "juliay";
        String juliaZoom_cmp = "juliazoom";
        String colourMode_cmp = "colourmode";
        String tasksPerWorker_cmp = "tasksperworker";
        String renderMode_cmp = "rendermode";
        String connection_cmp = "connection";
        ArrayList<String> param_list = new ArrayList<String>();

        String iterations = "128";
        String power = "2.0";
        String mandelbrotx = "0.0";
        String mandelbroty = "0.0";
        String mandelbrotzoom = "0.0";
        String juliax = "0.0";
        String juliay = "0.0";
        String juliazoom = "0.0";
        String colourmode = "BLACK_WHITE";
        String tasksperworker = "5";
        String rendermode = "LOCAL";
        String default_connection = "localhost:8010";

        Parameters params = getParameters();
        List<String> list = params.getRaw();

        for (String each : list) {
            //check for -- to start param
            StringBuilder compare_components = new StringBuilder();
            compare_components.append(each.charAt(0));
            compare_components.append(each.charAt(1));
            String compare_string = compare_components.toString();
            String param_start = "--";
            if (compare_string.equals(param_start)) {
                String only_input_name = each.substring(2);
                String[] attribute_and_value = only_input_name.split("=");
                int count = 0;
                for (String every : attribute_and_value) {
                    count = count + 1;
                }
                //check for more =
                if (count != 2) {
                    continue;
                }

                String case_sens_attr = attribute_and_value[0];
                //case insensitive
                String attribute = case_sens_attr.toLowerCase();
                String value = attribute_and_value[1];

                //isnumeric checks
                if (attribute.equals(iterations_cmp)) {
                    if (numericCheck(value)) {
                        iterations = value;
                    }
                } else if (attribute.equals(power_cmp)) {
                    if (numericCheck(value)) {
                        power = value;
                    }
                } else if (attribute.equals(mandelbrotX_cmp)) {
                    if (numericCheck(value)) {
                        mandelbrotx = value;
                    }
                } else if (attribute.equals(mandelbrotY_cmp)) {
                    if (numericCheck(value)) {
                        mandelbroty = value;
                    }
                } else if (attribute.equals(mandelbrotZoom_cmp)) {
                    if (numericCheck(value)) {
                        mandelbrotzoom = value;
                    }
                } else if (attribute.equals(juliaX_cmp)) {
                    if (numericCheck(value)) {
                        juliax = value;
                    }
                } else if (attribute.equals(juliaY_cmp)) {
                    if (numericCheck(value)) {
                        juliay = value;
                    }
                } else if (attribute.equals(juliaZoom_cmp)) {
                    if (numericCheck(value)) {
                        juliazoom = value;
                    }
                } else if (attribute.equals(colourMode_cmp)) {
                    if (value.equals("COLOUR_FADE")) {
                        colourmode = value;
                    }
                } else if (attribute.equals(tasksPerWorker_cmp)) {
                    if (numericCheck(value)) {
                        tasksperworker = value;
                    }
                } else if (attribute.equals(renderMode_cmp)) {
                    if (value.equals("LOCAL")) {
                        rendermode = value;
                    } else if (value.equals(("DISTRIBUTED"))) {
                        rendermode = value;
                    }
                } else if (attribute.equals(connection_cmp)) {
                    //"^" + ip_address_regex + ":" + port_regex + "$"
                    Pattern parse_connection_regex = Pattern.compile("^" + ip_address_regex + ":" + port_regex + "$");
                    String[] single_connections = value.split(",");
                    for (String single_connection : single_connections) {
                        Matcher connection_matcher = parse_connection_regex.matcher(single_connection);
                        if (connection_matcher.matches())
                        {
                            //System.out.println("Added con: " + single_connection);
                            connectionManager.addConnectionStringElement(single_connection);
                        }
                    }
                }
            }
        }

        if (connectionManager.getConnectionStringProperty().get().equals("")) {
            //System.out.println("Added con: " + default_connection);
            connectionManager.addConnectionStringElement(default_connection);
        }

        param_list.add(iterations);
        param_list.add(power);
        param_list.add(mandelbrotx);
        param_list.add(mandelbroty);
        param_list.add(mandelbrotzoom);
        param_list.add(juliax);
        param_list.add(juliay);
        param_list.add(juliazoom);
        param_list.add(colourmode);
        param_list.add(tasksperworker);
        param_list.add(rendermode);

        return param_list;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        ArrayList<String> param_list;
        param_list = parseParameters();
        time_thread.setDaemon(true);
        time_thread.start();

        ColourModes parsed_cm;
        RenderMode parsed_rm;

        if (param_list.get(8).equals("COLOUR_FADE")) {
            parsed_cm = ColourModes.COLOUR_FADE;
        } else if (param_list.get(8).equals("BLACK_WHITE")) {
            parsed_cm = ColourModes.BLACK_WHITE;
        } else {
            parsed_cm = ColourModes.CUSTOM;
        }

        if (param_list.get(10).equals("DISTRIBUTED")) {
            parsed_rm = RenderMode.DISTRIBUTED;
        } else {
            parsed_rm = RenderMode.LOCAL;
        }

        julia_instance.setCenterX(Double.parseDouble(param_list.get(5)));
        julia_instance.setCenterY(Double.parseDouble(param_list.get(6)));
        julia_instance.setWidth(1);
        julia_instance.setHeight(1);
        julia_instance.setZoom(Double.parseDouble(param_list.get(7)));
        julia_instance.setPower(Double.parseDouble(param_list.get(1)));
        julia_instance.setIterations(Integer.parseInt(param_list.get(0)));
        julia_instance.setRequestId(0);
        julia_instance.setTotalFragments(0);
        julia_instance.setFragmentNumber(0);
        julia_instance.setType(FractalType.JULIA);
        julia_instance.setMode(parsed_cm);
        julia_instance.setRenderMode(parsed_rm);
        julia_instance.setConstantX(0);
        julia_instance.setConstantY(0);

        mandel_instance.setCenterX(Double.parseDouble(param_list.get(2)));
        mandel_instance.setCenterY(Double.parseDouble(param_list.get(3)));
        mandel_instance.setWidth(1);
        mandel_instance.setHeight(1);
        mandel_instance.setZoom(Double.parseDouble(param_list.get(4)));
        mandel_instance.setPower(Double.parseDouble(param_list.get(1)));
        mandel_instance.setIterations(Integer.parseInt(param_list.get(0)));
        mandel_instance.setTotalFragments(0);
        mandel_instance.setFragmentNumber(0);
        mandel_instance.setType(FractalType.MANDELBROT);
        mandel_instance.setMode(parsed_cm);
        mandel_instance.setRenderMode(parsed_rm);
        //connectionManager.setConnectionString(param_list.get(11));
        taskPerWorkerProperty.set(Integer.parseInt(param_list.get(9)));

        julia_instance.constantXProperty().bind(mandel_instance.centerXProperty());
        julia_instance.constantYProperty().bind(mandel_instance.centerYProperty());

        FractalLogger.logArgumentsGUI(mandel_instance.centerXProperty(), mandel_instance.centerYProperty(), mandel_instance.zoomProperty(), mandel_instance.powerProperty(), mandel_instance.iterationsProperty(), julia_instance.centerXProperty(), julia_instance.centerYProperty(), julia_instance.zoomProperty(), ColorModeProperty.optionProperty());
        FractalLogger.logDistributionArgumentsGUI(RenderModeProperty.optionProperty(), taskPerWorkerProperty, connectionManager.getConnectionStringProperty());

        if(julia_instance.getRenderMode() == RenderMode.DISTRIBUTED
                || mandel_instance.getRenderMode() == RenderMode.DISTRIBUTED) {
            System.out.println("init creating workers because in dist mode");
            connectionManager.creatingWorkerConnections();
        }


        mainPane = new GridPane();


        leftCanvas = new Canvas();
        leftCanvas.setCursor(Cursor.HAND);

        mainPane.setGridLinesVisible(true);
        mainPane.add(leftCanvas, 0, 0);

        rightCanvas = new Canvas();
        rightCanvas.setCursor(Cursor.HAND);


        mainPane.add(rightCanvas, 1, 0);

        ColumnConstraints cc1 =
                new ColumnConstraints(100, 100, -1, Priority.ALWAYS, HPos.CENTER, true);
        ColumnConstraints cc2 =
                new ColumnConstraints(100, 100, -1, Priority.ALWAYS, HPos.CENTER, true);
        ColumnConstraints cc3 =
                new ColumnConstraints(400, 400, 400, Priority.ALWAYS, HPos.CENTER, true);

        mainPane.getColumnConstraints().addAll(cc1, cc2, cc3);


        RowConstraints rc1 =
                new RowConstraints(400, 400, -1, Priority.ALWAYS, VPos.CENTER, true);

        mainPane.getRowConstraints().addAll(rc1);

        leftHeight.bind(leftCanvas.heightProperty());
        leftWidth.bind(leftCanvas.widthProperty());
        rightHeight.bind(rightCanvas.heightProperty());
        rightWidth.bind(rightCanvas.widthProperty());

        mainPane.widthProperty().addListener(observable -> updateSizes());
        mainPane.heightProperty().addListener(observable -> updateSizes());


        controlPane = new GridPane();
        ColumnConstraints controlLabelColConstraint =
                new ColumnConstraints(195, 195, 200, Priority.ALWAYS, HPos.CENTER, true);
        ColumnConstraints controlControlColConstraint =
                new ColumnConstraints(195, 195, 195, Priority.ALWAYS, HPos.CENTER, true);
        controlPane.getColumnConstraints().addAll(controlLabelColConstraint, controlControlColConstraint);

        controlPane.setAlignment(Pos.TOP_CENTER);
        controlPane.setHgap(10);
        controlPane.setVgap(10);
        Insets padding1 = new Insets(10);
        controlPane.setPadding(padding1);

        //Title
        Text title = new Text("Fractal Input Values");
        title.setFont(Font.font("Roboto", FontWeight.NORMAL, 18));
        controlPane.add(title, 0, 0, 2, 1);

        Label iterations = new Label("Iterations");
        controlPane.add(iterations, 0, 1);
        TextField t1 = new TextField();
        t1.setText(param_list.get(0));
        controlPane.add(t1, 1, 1);

        Label power = new Label("Power");
        controlPane.add(power, 0, 2);
        TextField t2 = new TextField();
        t2.setText(param_list.get(1));
        controlPane.add(t2, 1, 2);

        Label mXcenter = new Label("Mandelbrot X center");
        controlPane.add(mXcenter, 0, 3);
        TextField t3 = new TextField();
        t3.setText(param_list.get(2));
        controlPane.add(t3, 1, 3);

        Label mYcenter = new Label("Mandelbrot Y center");
        controlPane.add(mYcenter, 0, 4);
        TextField t4 = new TextField();
        t4.setText(param_list.get(3));
        controlPane.add(t4, 1, 4);

        Label mandelbrot_zoom = new Label("Mandelbrot Zoom");
        controlPane.add(mandelbrot_zoom, 0, 5);
        TextField t5 = new TextField();
        t5.setText(param_list.get(4));
        controlPane.add(t5, 1, 5);

        Label julia_x_center = new Label("Julia X center");
        controlPane.add(julia_x_center, 0, 6);
        TextField t6 = new TextField();
        t6.setText(param_list.get(5));
        controlPane.add(t6, 1, 6);

        Label julia_y_center = new Label("Julia Y center");
        controlPane.add(julia_y_center, 0, 7);
        TextField t7 = new TextField();
        t7.setText(param_list.get(6));
        controlPane.add(t7, 1, 7);

        Label julia_zoom = new Label("Julia Zoom");
        controlPane.add(julia_zoom, 0, 8);
        TextField t8 = new TextField();
        t8.setText(param_list.get(7));
        controlPane.add(t8, 1, 8);

        Label colour_mode = new Label("Colour Mode");
        controlPane.add(colour_mode, 0, 9);
        ComboBox color_mode_box = new ComboBox();
        color_mode_box.getItems().add("BLACK_WHITE");
        color_mode_box.getItems().add("COLOUR_FADE");
        color_mode_box.getItems().add("CUSTOM");
        color_mode_box.getItems().add("DISCO_MODE");
        if (julia_instance.getMode() == ColourModes.BLACK_WHITE) {
            color_mode_box.getSelectionModel().select(0);
        } else {
            color_mode_box.getSelectionModel().select(1);
        }
        controlPane.add(color_mode_box, 1, 9);

        Label render_mode = new Label("Render Mode");
        controlPane.add(render_mode, 0, 10);
        ComboBox render_mode_box = new ComboBox();
        render_mode_box.getItems().add("LOCAL");
        render_mode_box.getItems().add("DISTRIBUTED");
        if (julia_instance.getRenderMode() == RenderMode.LOCAL) {
            render_mode_box.getSelectionModel().selectFirst();
        } else {
            render_mode_box.getSelectionModel().selectLast();
        }
        controlPane.add(render_mode_box, 1, 10);

        Label task_per_worker = new Label("Tasks per Worker");
        controlPane.add(task_per_worker, 0, 11);
        TextField t9 = new TextField();
        t9.setText(param_list.get(9));
        controlPane.add(t9, 1, 11);

        Button connection_editor_button = new Button("Connection Editor");
        controlPane.add(connection_editor_button, 1, 12);

        Label connected_workers = new Label("Connected Workers:");
        controlPane.add(connected_workers, 0, 13);
        Label connected_workers_output = new Label(String.valueOf(connectionManager.getNumberOfConnectedWorkers()));
        connectionManager.numberOfConnectedWorkers.addListener(observable -> {
            Platform.runLater(() -> {
                connected_workers_output.setText(String.valueOf(connectionManager.getNumberOfConnectedWorkers()));
            });
        });
        controlPane.add(connected_workers_output, 1, 13);

        //Bonus tasks
        Label bonus_label = new Label("Bonus:");
        controlPane.add(bonus_label, 0, 16);

        Label fractal_l = new Label("Second Fractal:");
        controlPane.add(fractal_l, 0, 17);
        ComboBox fractal_display_box = new ComboBox();
        fractal_display_box.getItems().add("JULIA");
        fractal_display_box.getItems().add("BURNING_SHIP");
        fractal_display_box.getItems().add("BURNING_JULIA");
        fractal_display_box.getItems().add("BURNING_BIRD");
        fractal_display_box.getItems().add("BURNING_JULIA_BIRD");
        fractal_display_box.getItems().add("NEWTON_POLY");
        fractal_display_box.getItems().add("NEWTON_SINE");
        fractal_display_box.getSelectionModel().selectFirst();
        controlPane.add(fractal_display_box, 1, 17);

        //Colorpickers
        Label cp1_label = new Label("Color 1:");
        controlPane.add(cp1_label, 0, 18);
        final ColorPicker colorPickerColor1 = new ColorPicker();
        colorPickerColor1.setValue(Color.RED);
        Colours.setColour1(colorPickerColor1.getValue().toString());
        controlPane.add(colorPickerColor1, 0, 19);

        Label cp2_label = new Label("Color 2:");
        controlPane.add(cp2_label, 1, 18);
        final ColorPicker colorPickerColor2 = new ColorPicker();
        colorPickerColor2.setValue(Color.BLUE);
        Colours.setColour2(colorPickerColor2.getValue().toString());
        controlPane.add(colorPickerColor2, 1, 19);

        Label progress_lb = new Label("Render Progress: ");
        controlPane.add(progress_lb, 0, 20);
        ProgressBar pb = new ProgressBar(0);
        controlPane.add(pb, 1,20);

        Label timestamp_lb = new Label("Time for last render call: ");
        controlPane.add(timestamp_lb, 0, 21);
        long test = 0;
        String test_str = test+" ms";
        Label timestamp_value = new Label(test_str);
        controlPane.add(timestamp_value, 1, 21);


        //Bindings
        //TODO: set Workerstuff
        //iterations

        t1.focusedProperty().addListener(observable -> {
            if (parseTextField(t1))
            {
                t1.textProperty().set(String.valueOf(128));
            }
            if (Integer.parseInt(t1.textProperty().get()) < 1)
            {
                t1.textProperty().set(String.valueOf(128));
            }
        });
        t2.focusedProperty().addListener(observable -> {
            if (parseTextField(t2))
            {
                t2.textProperty().set(String.valueOf(2.0));
            }
            if (Double.parseDouble(t2.textProperty().get()) < 2.0)
            {
                t2.textProperty().set(String.valueOf(2.0));
            }

        });
        t3.focusedProperty().addListener(observable -> {
            if (parseTextField(t3))
            {
                t3.textProperty().set(String.valueOf(0.0));
            }
        });
        t4.focusedProperty().addListener(observable -> {
            if (parseTextField(t4))
            {
                t4.textProperty().set(String.valueOf(0.0));
            }
        });
        t5.focusedProperty().addListener(observable -> {
            if (parseTextField(t5))
            {
                t5.textProperty().set(String.valueOf(0.0));
            }
        });
        t6.focusedProperty().addListener(observable -> {
            if (parseTextField(t6))
            {
                t6.textProperty().set(String.valueOf(0.0));
            }
        });
        t7.focusedProperty().addListener(observable -> {
            if (parseTextField(t7))
            {
                t7.textProperty().set(String.valueOf(0.0));
            }
        });
        t8.focusedProperty().addListener(observable -> {
            if (parseTextField(t8))
            {
                t8.textProperty().set(String.valueOf(0.0));
            }
        });

        julia_instance.iterationsProperty().addListener(observable -> {
            TimerThread.setRequestNewImage(3);
        });
        julia_instance.powerProperty().addListener(observable -> {
            TimerThread.setRequestNewImage(3);
        });
        mandel_instance.centerXProperty().addListener(observable -> {
            FractalLogger.logDragGUI(mandel_instance.getCenterX(), mandel_instance.getCenterY(), mandel_instance.getType());
            TimerThread.setRequestNewImage(3);
        });
        mandel_instance.centerYProperty().addListener(observable -> {
            FractalLogger.logDragGUI(mandel_instance.getCenterX(), mandel_instance.getCenterY(), mandel_instance.getType());
            TimerThread.setRequestNewImage(3);
        });
        mandel_instance.zoomProperty().addListener(observable -> {
            FractalLogger.logZoomGUI(mandel_instance.getZoom(), mandel_instance.getType());
            TimerThread.setRequestNewImage(1);
        });
        julia_instance.centerXProperty().addListener(observable -> {
            FractalLogger.logDragGUI(julia_instance.getCenterX(), julia_instance.getCenterY(), julia_instance.getType());
            TimerThread.setRequestNewImage(2);
        });
        julia_instance.centerYProperty().addListener(observable -> {
            FractalLogger.logDragGUI(julia_instance.getCenterX(), julia_instance.getCenterY(), julia_instance.getType());
            TimerThread.setRequestNewImage(2);
        });
        julia_instance.zoomProperty().addListener(observable -> {
            FractalLogger.logZoomGUI(julia_instance.getZoom(), julia_instance.getType());
            TimerThread.setRequestNewImage(2);
        });

        t1.textProperty().bindBidirectional(julia_instance.iterationsProperty(), getConverter(t1, 128));
        t1.textProperty().bindBidirectional(mandel_instance.iterationsProperty(), getConverter(t1, 128));
        t2.textProperty().bindBidirectional(julia_instance.powerProperty(), getConverter(t2, 2.0d));
        t2.textProperty().bindBidirectional(mandel_instance.powerProperty(), getConverter(t2, 2.0d));
        t3.textProperty().bindBidirectional(mandel_instance.centerXProperty(), getConverter(t3, 0d));
        t4.textProperty().bindBidirectional(mandel_instance.centerYProperty(), getConverter(t4, 0d));
        t5.textProperty().bindBidirectional(mandel_instance.zoomProperty(), getConverter(t5, 0d));
        t6.textProperty().bindBidirectional(julia_instance.centerXProperty(), getConverter(t6, 0d));
        t7.textProperty().bindBidirectional(julia_instance.centerYProperty(), getConverter(t7, 0d));
        t8.textProperty().bindBidirectional(julia_instance.zoomProperty(), getConverter(t8, 0d));


        //Colour mode
        color_mode_box.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            if (newValue == "COLOUR_FADE") {
                julia_instance.setMode(ColourModes.COLOUR_FADE);
                mandel_instance.setMode(ColourModes.COLOUR_FADE);
            } else if (newValue == "BLACK_WHITE") {
                julia_instance.setMode(ColourModes.BLACK_WHITE);
                mandel_instance.setMode(ColourModes.BLACK_WHITE);
            } else if (newValue == "CUSTOM") {
                Colours.setColour1(colorPickerColor1.getValue().toString());
                Colours.setColour2(colorPickerColor2.getValue().toString());
                julia_instance.setMode(ColourModes.CUSTOM);
                mandel_instance.setMode(ColourModes.CUSTOM);
            } else {
                julia_instance.setRenderMode(RenderMode.LOCAL);
                mandel_instance.setRenderMode(RenderMode.LOCAL);
                julia_instance.setMode(ColourModes.DISCO_MODE);
                mandel_instance.setMode(ColourModes.DISCO_MODE);
                Platform.runLater(() -> {FractalApplication.renderModeSelectionBox.getSelectionModel().selectFirst();});
            }
            //System.out.println(julia_instance.getMode());
            TimerThread.setRequestNewImage(3);
        });

        //Fractal
        fractal_display_box.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            if (newValue == "JULIA") {
                julia_instance.setType(FractalType.JULIA);
            } else if (newValue == "BURNING_SHIP") {
                julia_instance.setType(FractalType.BURNING_SHIP);
            } else if (newValue == "BURNING_JULIA") {
                julia_instance.setType(FractalType.BURNING_JULIA);
            } else if (newValue == "BURNING_BIRD") {
                julia_instance.setType(FractalType.BURNING_BIRD);
            } else if (newValue == "BURNING_JULIA_BIRD"){
                julia_instance.setType(FractalType.BURNING_JULIA_BIRD);
            } else if (newValue == "NEWTON_SINE") {
                julia_instance.setType(FractalType.NEWTON_SINE);
            } else {
                julia_instance.setType(FractalType.NEWTON_POLY);
            }
            //System.out.println(julia_instance.getType());
            TimerThread.setRequestNewImage(2);
        });

        //Render mode
        render_mode_box.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            if (newValue == "LOCAL") {
                julia_instance.setRenderMode(RenderMode.LOCAL);
                mandel_instance.setRenderMode(RenderMode.LOCAL);
                new Thread(() -> {
                    try {
                        connectionManager.dropConnections();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            } else {
                if (color_mode_box.getSelectionModel().getSelectedItem().toString().equals("DISCO_MODE")){
                    Platform.runLater(() -> {FractalApplication.renderModeSelectionBox.getSelectionModel().selectFirst();});
                }
                else {
                    julia_instance.setRenderMode(RenderMode.DISTRIBUTED);
                    mandel_instance.setRenderMode(RenderMode.DISTRIBUTED);
                    if (connectionManager.getNumberOfWorkers() == 0){
                        new Thread(() -> {
                            try {
                                System.out.println("creating to workers");
                                connectionManager.creatingWorkerConnections();
                            } catch (IOException | InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                }
            }
            TimerThread.setRequestNewImage(3);
        });

        //ColorPicker1
        colorPickerColor1.valueProperty().addListener(observable -> {
            Colours.setColour1(colorPickerColor1.getValue().toString());
            TimerThread.setRequestNewImage(3);
        });
        //ColorPicker2
        colorPickerColor2.valueProperty().addListener(observable -> {
            Colours.setColour2(colorPickerColor2.getValue().toString());
            TimerThread.setRequestNewImage(3);
        });

        this.renderModeSelectionBox = render_mode_box;
        this.renderProgressBar = pb;
        this.timestamp_label = timestamp_value;

        //Workers
        t9.textProperty().addListener(observable -> {
            //something for anton? i guess.. i have no idea
            taskPerWorkerProperty.set(Integer.parseInt(t9.getText()));
            //new render?
        });
        taskPerWorkerProperty.addListener(observable -> {
            t9.setText(String.valueOf(taskPerWorkerProperty.get()));
        });

        leftCanvas.addEventFilter(ScrollEvent.SCROLL_STARTED, e ->{
            synchronized (TimerThread.panning_activated) {
                time_thread.zoom_activated.set(true);
            }
        });

        //change zoom by 0.02 when scrolling julia
        rightCanvas.addEventHandler(ScrollEvent.SCROLL, event -> {
            julia_instance.setZoom(julia_instance.getZoom() + event.getDeltaY() * 0.02d);
        });
        //change zoom by 0.02 when scrolling mandel
        leftCanvas.addEventHandler(ScrollEvent.SCROLL, event -> {
            mandel_instance.setZoom(mandel_instance.getZoom() + event.getDeltaY() * 0.02d);
        });

        AtomicReference<Double> last_mouse_X = new AtomicReference<Double>();
        AtomicReference<Double>last_mouse_Y = new AtomicReference<Double>();

        rightCanvas.addEventFilter(MouseEvent.MOUSE_PRESSED, event ->{
            last_mouse_X.set(event.getX());
            last_mouse_Y.set(event.getY());
            synchronized (TimerThread.panning_activated) {
                time_thread.panning_activated.set(true);
            }
        });

        rightCanvas.addEventFilter(MouseEvent.MOUSE_RELEASED, e ->{
            synchronized (TimerThread.panning_activated) {
                time_thread.panning_activated.set(false);
            }
        });

        leftCanvas.addEventFilter(MouseEvent.MOUSE_PRESSED, e ->{
            last_mouse_X.set(e.getX());
            last_mouse_Y.set(e.getY());
            synchronized (TimerThread.panning_activated) {
                time_thread.panning_activated.set(true);
            }
        });

        leftCanvas.addEventFilter(MouseEvent.MOUSE_RELEASED, e ->{
            synchronized (TimerThread.panning_activated) {
                time_thread.panning_activated.set(false);
            }
        });

        rightCanvas.addEventFilter(MouseEvent.MOUSE_DRAGGED, event ->{
            //JuliaRenderOptions;
            double[] before = {last_mouse_X.get(),last_mouse_Y.get()};
            double[] after = {event.getX(), event.getY()};
            double[] difference = panningDifference(time_thread.getCurrentJulia(), before, after);
            julia_instance.setCenterX(julia_instance.getCenterX() - difference[0]);
            julia_instance.setCenterY(julia_instance.getCenterY() - difference[1]);
            last_mouse_X.set(event.getX());
            last_mouse_Y.set(event.getY());
        });

        leftCanvas.addEventFilter(MouseEvent.MOUSE_DRAGGED, event ->{
            //MandelRenderOptions;
            double[] before = {last_mouse_X.get(),last_mouse_Y.get()};
            double[] after = {event.getX(), event.getY()};
            double[] difference = panningDifference(time_thread.getCurrentMandel(), before, after);
            mandel_instance.setCenterX(mandel_instance.getCenterX() - difference[0]);
            mandel_instance.setCenterY(mandel_instance.getCenterY() - difference[1]);
            last_mouse_X.set(event.getX());
            last_mouse_Y.set(event.getY());
        });


        //add new connection
        Stage add_new_connection = new Stage();
        VBox add_new_connection_box = new VBox(10);
        ListView<String> connections_list = new ListView<String>();
        List<String> temp = connectionManager.getConnectionStringElements();
        connections_list.getItems().addAll(temp);
        //end

        //Connection handler
        Text connection_text = new Text("Worker Connections:");
        Text enter_connection_text = new Text("Enter new Connection:");
        Text connection_feedback_text = new Text("");
        TextField enter_connection_field = new TextField();
        Button plus_button = new Button("+");
        Button minus_button = new Button("-");
        Button check_connections_button = new Button("Check connections");
        Button cancel_button = new Button("Cancel");
        Button save_button = new Button("Save");
        save_button.setStyle("-fx-background-color: #484349; -fx-text-fill: #ffffff");
        Stage connection_editor = new Stage();

        GridPane connection_editor_box = new GridPane();
        connection_editor_box.setHgap(10);
        connection_editor_box.setVgap(10);
        ColumnConstraints column_constraint_editor = new ColumnConstraints();
        column_constraint_editor.setPercentWidth(50);
        RowConstraints row_constraint_editor = new RowConstraints();
        row_constraint_editor.setPercentHeight(5);
        connection_editor_box.getColumnConstraints().add(column_constraint_editor);
        connection_editor_box.getRowConstraints().add(row_constraint_editor);

        connection_editor_box.setPadding(new Insets(10));
        connection_editor.initOwner(primaryStage);
        Scene connection_scene = new Scene(connection_editor_box, 300, 700);
        connection_editor.setScene(connection_scene);

        connection_editor_box.add(connection_text, 0, 1);
        connection_editor_box.add(connections_list, 0, 2, 2, 1);
        connections_list.setPrefSize(120,500);
        connection_editor_box.add(plus_button, 0, 3);
        connection_editor_box.add(minus_button, 0, 4);
        connection_editor_box.add(check_connections_button, 1, 3);
        connection_editor_box.add(cancel_button, 0, 5);
        connection_editor_box.add(save_button, 1, 5);



        //plus button
        add_new_connection.initOwner(connection_editor);
        add_new_connection_box.getChildren().add(enter_connection_text);
        add_new_connection_box.getChildren().add(enter_connection_field);
        add_new_connection_box.getChildren().add(connection_feedback_text);
        Scene enter_connection_scene = new Scene(add_new_connection_box, 250, 100);
        add_new_connection.setScene(enter_connection_scene);


        //Connection result
        Text connection_status = new Text("All Connections work!");
        Text connection_number = new Text(" were connected!");
        Button ok_button = new Button("Ok");
        ok_button.setStyle("-fx-background-color: #00AFB9; -fx-text-fill: #ffffff");
        GridPane connection_result_gridpane = new GridPane();
        connection_result_gridpane.add(connection_status, 0, 0);
        connection_result_gridpane.add(connection_number, 0, 1);
        connection_result_gridpane.add(ok_button, 2, 2);
        Scene connection_result_scene = new Scene(connection_result_gridpane, 200, 100);
        Stage connection_result_stage = new Stage();
        connection_result_stage.setScene(connection_result_scene);


        connection_editor_button.setOnAction(e -> {
            save_button.setStyle("-fx-background-color: #484349; -fx-text-fill: #ffffff");
            connection_editor.show();

        });
        List<String> temp_inputs = new ArrayList<String>();
        plus_button.setOnAction(e->{
            add_new_connection.show();
            connection_feedback_text.setText("");
            save_button.setStyle("-fx-background-color: #00AFB9; -fx-text-fill: #ffffff");
        });
        minus_button.setOnAction(event->{
            connections_list.getItems().remove(connections_list.getSelectionModel().getSelectedItem());
            try {
                connectionManager.dropConnection(connections_list.getSelectionModel().getSelectedItem());
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            save_button.setStyle("-fx-background-color: #00AFB9; -fx-text-fill: #ffffff");
        });
        //"^" + ip_address_regex + ":" + port_regex + "$"
        Pattern connection_regex = Pattern.compile("^" + ip_address_regex + ":" + port_regex + "$");
        /*
        primaryStage.maximizedProperty().addListener(event->{
            System.out.println("MACXIMIED");
            updateSizes();
        });
        */

        enter_connection_field.setOnAction(e->{
            String input = enter_connection_field.getText();
            Matcher connection_matcher = connection_regex.matcher(input);
            if (connection_matcher.matches())
            {
                temp_inputs.add(input);
                connections_list.getItems().add(input);
                enter_connection_field.setText("");
                connection_feedback_text.setText("Connection was successfully added!");
            }
            else {
                temp_inputs.add("localhost:8010");
                connections_list.getItems().add("localhost:8010");
                enter_connection_field.setText("");
                connection_feedback_text.setText("Invalid input");
            }
        });
        check_connections_button.setOnAction(e->{
            connection_status.setText("Checking connections...");
            connection_number.setText("Please wait");
            connection_result_stage.show();

            new Thread(() -> {
                try {
                    connectionManager.checkWorkerConnections(true);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }

                if (connectionManager.getNumberOfConnectedWorkers() > 0)
                {
                    connection_status.setText("All Connections work!");
                }
                else
                {
                    connection_status.setText("Connections dont work");
                }
                connected_workers_output.setText(String.valueOf(connectionManager.getNumberOfConnectedWorkers()));
                connection_number.setText(connectionManager.getNumberOfConnectedWorkers() +"/" + connectionManager.getNumberOfWorkers() + " were connected!");
            }).start();
        });
        ok_button.setOnAction(e->{
            connection_result_stage.hide();
        });

        cancel_button.setOnAction(e->{
            //discard changes
            for (String s : temp_inputs)
            {
                connections_list.getItems().remove(s);
            }
            temp_inputs.removeAll(temp_inputs);
            connection_editor.hide();
        });
        connection_editor.setOnCloseRequest(windowEvent -> {
            for (String s : temp_inputs)
            {
                connections_list.getItems().remove(s);
            }
            temp_inputs.removeAll(temp_inputs);
            save_button.setStyle("-fx-background-color: #484349; -fx-text-fill: #ffffff");
        });
        save_button.setOnAction(e->{
            for (String s : temp_inputs)
            {
                try {
                    connectionManager.addConnectionStringElement(s);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
            temp_inputs.removeAll(temp_inputs);
            new Thread(() -> {
                try {
                    connectionManager.checkWorkerConnections(true);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }).start();
            save_button.setStyle("-fx-background-color: #484349; -fx-text-fill: #ffffff");
        });



        //d end
        mainPane.add(controlPane, 2, 0);


        Scene scene = new Scene(mainPane);

        primaryStage.setTitle("Fractal Displayer");

        primaryStage.addEventHandler(WindowEvent.WINDOW_SHOWING, event -> {
            updateSizes();
        });
        primaryStage.addEventHandler(WindowEvent.WINDOW_SHOWN, event -> {
            updateSizes();
        });
        primaryStage.addEventHandler(WindowEvent.ANY, event -> {
            updateSizes();
        });

        // MAXIMIZE
        /* doesn't work
        primaryStage.heightProperty().addListener(event->{
            updateSizes();
            System.out.println("CHANGE");
        });
        primaryStage.widthProperty().add(event->{
            System.out.println("CHANGE");
            updateSizes();
        });
        */

        primaryStage.setWidth(1080);
        primaryStage.setHeight(720);


        primaryStage.setScene(scene);
        primaryStage.show();
        updateSizes();

        Platform.runLater(() -> {
            updateSizes();
            FractalLogger.logInitializedGUI(mainPane, primaryStage, leftCanvas, rightCanvas);
        });
    }

    public static void displayLeftImage(SimpleImage image) {
        Platform.runLater(() -> {
            fillCanvas(CanvasSide.LEFT, image);
        });
    }

    public static void displayRightImage(SimpleImage image) {
        Platform.runLater(() -> {
            fillCanvas(CanvasSide.RIGHT, image);
        });
    }

    private boolean parseTextField(TextField textfield)
    {
        if (textfield.textProperty().get() == "")
        {
            return true;
        }
        if (textfield.textProperty().get().equals("-"))
        {
            return true;
        }
        return false;
    }

    private StringConverter<Number> getConverter(TextField textField, Number standard_value) {
        return new StringConverter<Number>() {
            @Override
            public String toString(Number number) {
                if (number == null)
                {
                    return String.valueOf(standard_value);
                }
                try{
                    String.valueOf(number);
                }
                catch(Exception e){
                    return String.valueOf(standard_value);
                }
                return String.valueOf(number);
                //return null;
            }

            @Override
            public Number fromString(String s) {
                Double input;
                Integer intput;
                if (s == "")
                {
                    //textField.textProperty().set(String.valueOf(standard_value));
                    return standard_value;
                }
                if (s.equals("-"))
                {
                    //textField.textProperty().set(String.valueOf(standard_value));

                    return standard_value;
                }
                if (standard_value.equals(128))
                {
                    try{
                        intput = Integer.parseInt(s);
                    }
                    catch(Exception e){
                        Platform.runLater(()->{
                            textField.textProperty().set(String.valueOf(standard_value));
                        });
                        return standard_value;
                    }
                    if (intput < 1)
                    {
                        return standard_value;
                    }
                }

                try{
                    input = Double.parseDouble(s);
                }
                catch(Exception e){
                    Platform.runLater(()->{
                        textField.textProperty().set(String.valueOf(standard_value));
                    });
                    return standard_value;
                }

                if (standard_value.equals(2.0))
                {
                    if (input < 2.0)
                    {
                        return standard_value;
                    }
                }
                return input;
            }
        };
    }

    public static void updateRenderMode(RenderMode mode)
    {
        if (FractalApplication.renderModeSelectionBox != null)
        {
            if (mode == RenderMode.LOCAL) {
                Platform.runLater(() -> {FractalApplication.renderModeSelectionBox.getSelectionModel().selectFirst();});
            }
            if (mode == RenderMode.DISTRIBUTED) {
                Platform.runLater(() -> {FractalApplication.renderModeSelectionBox.getSelectionModel().selectLast();});
            }
        }
    }
}
