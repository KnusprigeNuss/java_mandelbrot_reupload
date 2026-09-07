package at.tugraz.oop2.worker;

import at.tugraz.oop2.shared.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Connection implements Runnable {
    int id;
    ServerSocket serverSocket;
    boolean free = true;
    private final static int threadPoolSize = 100;
    private static SimpleImage[] mandelSubImages = new SimpleImage[threadPoolSize];
    private static SimpleImage[] juliaSubImages = new SimpleImage[threadPoolSize];

    public Connection(ServerSocket serverSocket, int id) {
        this.id = id;
        this.serverSocket = serverSocket;
    }
    @SuppressWarnings("All")
    public void run() {

        try {

            //System.out.println("(CID: "+id+") Server is listening");
            Socket clientSocket = serverSocket.accept();
            this.free = false;
            FractalLogger.logConnectionOpenedWorker();
            ObjectOutputStream objOutStream = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream objInStream = new ObjectInputStream(clientSocket.getInputStream());

            while (!clientSocket.isClosed())
            {
                try {
                    clientSocket.getOutputStream().write(0);
                }
                catch (IOException exception) {
                    clientSocket.close();
                    break;
                }

                // Connection alive
                int i = 0;
                try {
                    i = (int) objInStream.readObject();
                }
                catch (EOFException | SocketException ignored){
                    clientSocket.close();
                    // LOG WITH FLOGGER
                    break;
                }

                if (i == 1)
                {
                    // Render mandelbrot image
                    //System.out.println("WORKER " + this.id + " RENDERING MANDEL");
                    MandelbrotRenderOptions fractalRenderOptions = (MandelbrotRenderOptions) objInStream.readObject();
                    FractalLogger.logReceivePackageWorker(fractalRenderOptions);
                    int numberOfTasks = (int) objInStream.readObject();
                    FractalLogger.logReceivePackageWorker(fractalRenderOptions);
                    List<Integer> sizes = new ArrayList<>();
                    for (int j = 0; j < numberOfTasks; j++){
                        sizes.add(((int) objInStream.readObject()));
                        FractalLogger.logReceivePackageWorker(fractalRenderOptions);
                    }
                    int y_worker_order = (int) objInStream.readObject();
                    FractalLogger.logReceivePackageWorker(fractalRenderOptions);
                    int y_size = (int) objInStream.readObject();
                    FractalLogger.logReceivePackageWorker(fractalRenderOptions);
                    renderImages(fractalRenderOptions, y_size, sizes, (y_worker_order * y_size * numberOfTasks));
                    for (int j = 0; j < numberOfTasks; j++){
                        objOutStream.writeObject(mandelSubImages[j]);
                    }
                    //System.out.println("WORKER " + this.id + " DONE WITH MANDEL");
                }
                else if (i == 2){
                    //System.out.println("WORKER " + this.id + " RENDERING JULIA");
                    JuliaRenderOptions fractalRenderOptions = (JuliaRenderOptions) objInStream.readObject();
                    FractalLogger.logReceivePackageWorker(fractalRenderOptions);
                    int numberOfTasks = (int) objInStream.readObject();
                    FractalLogger.logReceivePackageWorker(fractalRenderOptions);
                    List<Integer> sizes = new ArrayList<>();
                    for (int j = 0; j < numberOfTasks; j++){
                        sizes.add(((int) objInStream.readObject()));
                        FractalLogger.logReceivePackageWorker(fractalRenderOptions);
                    }
                    int y_worker_order = (int) objInStream.readObject();
                    FractalLogger.logReceivePackageWorker(fractalRenderOptions);
                    int y_size = (int) objInStream.readObject();
                    FractalLogger.logReceivePackageWorker(fractalRenderOptions);
                    renderImages(fractalRenderOptions, y_size, sizes, (y_worker_order * y_size * numberOfTasks));
                    for (int j = 0; j < numberOfTasks; j++){
                        objOutStream.writeObject(juliaSubImages[j]);
                    }
                    //System.out.println("WORKER " + this.id + " DONE WITH JULIA");
                }
                //System.out.println("(CID: "+id+") Connection still alive");
                Thread.sleep(80);
            }
            FractalLogger.logConnectionLostWorker();
        }
        catch (IOException exception)
        {
            exception.printStackTrace();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private void renderImages(FractalRenderOptions fractalRenderOptions, int y_size, List<Integer> sizes, int start)
            throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        List<Future<?>> renderFutures = new ArrayList<>();
        List<RenderTaskSubImage> renderTasks = new ArrayList<>();
        mandelSubImages = new SimpleImage[threadPoolSize];
        juliaSubImages = new SimpleImage[threadPoolSize];

        for (int i = 0; i < sizes.size(); i++){
            renderTasks.add(new RenderTaskSubImage(fractalRenderOptions, i, y_size, sizes.get(i), start));
            renderFutures.add(executorService.submit(renderTasks.get(renderTasks.size()-1)));
        }

        while (true)
        {
            boolean allDone = true;
            for (Future<?> renderFuture : renderFutures) {
                if (!renderFuture.isCancelled() && !renderFuture.isDone()) { allDone = false; }
            }
            if (allDone) {
                break;
            }
        }

        executorService.shutdown();
    }

    private static class RenderTaskSubImage implements Runnable {
        private final FractalRenderOptions fractalRenderOptions;
        public boolean deprecated = false;
        private final Object deprecatedLock = new Object();
        int y_order;
        int y_size;
        int y_start;
        int size;
        public RenderTaskSubImage(FractalRenderOptions fractalRenderOptions, int y_order, int y_size, int size, int y_start) {
            this.fractalRenderOptions = fractalRenderOptions;
            this.y_order = y_order;
            this.y_size = y_size;
            this.y_start = y_start;
            this.size = size;
        }

        @Override
        public void run() {
            // RENDERING
            SimpleImage subImage = new SimpleImage(fractalRenderOptions.getWidth(), size);

            if (fractalRenderOptions.getType() == FractalType.MANDELBROT)
            {
                subImage.mandelbrotCalculation((MandelbrotRenderOptions) fractalRenderOptions,
                        y_start + y_order * y_size);
            }

            else /*if (fractalRenderOptions.getType() == FractalType.JULIA)*/
            {
                subImage.juliaCalculation((JuliaRenderOptions) fractalRenderOptions,
                        y_start + y_order * y_size);
            }

            // DISCARD IMAGE IF DEPRECATED
            synchronized (deprecatedLock)
            {
                if (!deprecated){
                    if (fractalRenderOptions.getType() == FractalType.MANDELBROT)
                    {
                        mandelSubImages[y_order] = subImage;
                    }
                    else /*if (fractalRenderOptions.getType() == FractalType.JULIA)*/
                    {
                        juliaSubImages[y_order] = subImage;
                    }
                }
            }
        }
    }
}
