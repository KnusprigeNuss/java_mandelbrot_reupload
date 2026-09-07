package at.tugraz.oop2.shared;

import javafx.beans.property.SimpleBooleanProperty;
import lombok.Getter;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class WorkerConnection implements Runnable {

    @Getter
    Socket connection = null;
    @Getter
    FractalRenderOptions fractalRenderOptions = null;
    private ObjectOutputStream objOutStream = null;
    private ObjectInputStream objInStream = null;
    @Getter
    String connectionString;
    public boolean timedOut = false;
    List<SimpleImage> renderedImages = new ArrayList<>();
    List<Integer> sizes = new ArrayList<>();
    public int y_worker_order;
    int y_size;
    private final SimpleBooleanProperty renderingFinished = new SimpleBooleanProperty();
    private int failedConnectionAttempts = 0;

    public WorkerConnection(String connectionString) {
        this.connectionString = connectionString;
        this.renderingFinished.set(false);
        System.out.println("Created worker: " + connectionString);
    }

    public void setRenderingFinished(boolean b){
        synchronized (this.renderingFinished){
            this.renderingFinished.set(b);
        }
    }

    public List<SimpleImage> renderImage(FractalRenderOptions fractalRenderOptions,
                                                      int y_worker_order, List<Integer> sizes,
                                                      int y_size)
            throws InterruptedException {

        this.renderedImages.clear();
        this.sizes.clear();
        this.y_worker_order = y_worker_order;
        this.y_size = y_size;
        this.fractalRenderOptions = fractalRenderOptions;
        this.sizes = sizes;
        boolean temp = false;

        int counter = 0;
        while (!temp){
            synchronized (this.renderingFinished){
                temp = this.renderingFinished.get();
            }
            counter++;
            Thread.sleep(20);
            if (counter == 200){
                List<SimpleImage> empty = new ArrayList<>(0);
                this.timedOut = true;
                return empty;
            }
        }

        return this.renderedImages;

    }

    public void closeConnection() throws IOException {
        if (connection != null)
        {
            this.connection.close();
            this.connection = null;
        }
        this.timedOut = true;
    }

    @SuppressWarnings("All")
    public void run() {
        try {
            //System.out.println("started worker with: " + this.connectionString);
            String[] workerData = this.connectionString.split(":");

            while (true)
            {
                Thread.sleep(200);
                if (failedConnectionAttempts >= 19) {
                    break;
                }

                if (this.connection == null)
                {
                    if (timedOut) {
                        break;
                    }
                    try {
                        //System.out.println(workerData);
                        this.connection = new Socket(workerData[0], Integer.parseInt(workerData[1]));
                    } catch (IOException ignored) {}
                    if (this.connection == null) {
                        // Reconnecting, if could not reconnect after 10 sec, abandon.
                        // System.out.println("RECONNECTING1: " + connectionString);
                        failedConnectionAttempts++;
                        continue;
                    }
                    else {
                        this.objOutStream = new ObjectOutputStream(connection.getOutputStream());
                        this.objInStream = new ObjectInputStream(connection.getInputStream());
                        FractalLogger.logConnectionOpenedGUI(workerData[0], Integer.parseInt(workerData[1]));
                    }
                }

                try {
                    if (this.connection.getInputStream().read() == -1)
                    {
                        this.connection.close();
                        this.connection = null;
                        //System.out.println("RECONNECTING2: " + connectionString);
                        failedConnectionAttempts++;
                        continue;
                    }
                }
                catch (IOException exception) {
                    this.connection.close();
                    this.connection = null;
                    //System.out.println("RECONNECTING3: " + connectionString);
                    failedConnectionAttempts++;
                    continue;
                }


                // Working connection
                failedConnectionAttempts = 0;
                boolean temp;
                synchronized (this.renderingFinished)
                {
                    temp = this.renderingFinished.get();
                }
                if (!temp)
                {
                    int total = 4 + sizes.size();
                    int count = 1;
                    if (this.fractalRenderOptions != null){
                        if (this.fractalRenderOptions.getType() == FractalType.MANDELBROT){
                            this.objOutStream.writeObject(1);
                            FractalLogger.logSendPackageGUI(fractalRenderOptions, count, total);
                            count++;
                            this.objOutStream.writeObject((MandelbrotRenderOptions) this.fractalRenderOptions);
                            FractalLogger.logSendPackageGUI(fractalRenderOptions, count, total);
                            count++;
                            this.objOutStream.writeObject(this.sizes.size());
                            for (int size : sizes){
                                FractalLogger.logSendPackageGUI(fractalRenderOptions, count, total);
                                count++;
                                this.objOutStream.writeObject(size);
                            }
                            FractalLogger.logSendPackageGUI(fractalRenderOptions, count, total);
                            count++;
                            this.objOutStream.writeObject(this.y_worker_order);
                            FractalLogger.logSendPackageGUI(fractalRenderOptions, count, total);
                            count++;
                            this.objOutStream.writeObject(this.y_size);
                            for (int size : sizes){
                                try {
                                    this.renderedImages.add((SimpleImage) this.objInStream.readObject());
                                    FractalLogger.logReceivePackageGUI(fractalRenderOptions);
                                }
                                catch (EOFException ignore) {
                                    FractalLogger.logFailedPackageGUI(fractalRenderOptions);
                                }
                            }
                            synchronized (this.renderingFinished){
                                this.renderingFinished.set(true);
                            }
                        }
                        else /*if (this.fractalRenderOptions.getType() == FractalType.JULIA)*/{
                            this.objOutStream.writeObject(2);
                            FractalLogger.logSendPackageGUI(fractalRenderOptions, count, total);
                            count++;
                            this.objOutStream.writeObject((JuliaRenderOptions) this.fractalRenderOptions);
                            FractalLogger.logSendPackageGUI(fractalRenderOptions, count, total);
                            count++;
                            this.objOutStream.writeObject(this.sizes.size());
                            for (int size : sizes){
                                FractalLogger.logSendPackageGUI(fractalRenderOptions, count, total);
                                count++;
                                this.objOutStream.writeObject(size);
                            }
                            FractalLogger.logSendPackageGUI(fractalRenderOptions, count, total);
                            count++;
                            this.objOutStream.writeObject(this.y_worker_order);
                            FractalLogger.logSendPackageGUI(fractalRenderOptions, count, total);
                            count++;
                            this.objOutStream.writeObject(this.y_size);
                            for (int size : sizes){
                                try {
                                    this.renderedImages.add((SimpleImage) this.objInStream.readObject());
                                    FractalLogger.logReceivePackageGUI(fractalRenderOptions);
                                }
                                catch (Exception ignored) {
                                    FractalLogger.logFailedPackageGUI(fractalRenderOptions);
                                }
                            }
                            synchronized (this.renderingFinished){
                                this.renderingFinished.set(true);
                            }
                        }
                        this.fractalRenderOptions = null;
                    }
                    else {
                        this.objOutStream.writeObject(0);
                    }
                }
            }

            if (this.connection != null)
            {
                this.connection.close();
                this.connection = null;
            }
            this.timedOut = true;
            FractalLogger.logConnectionLostGUI(workerData[0], Integer.parseInt(workerData[1]));
        }
        catch (IOException exception)
        {
            exception.printStackTrace();
        } catch (InterruptedException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
