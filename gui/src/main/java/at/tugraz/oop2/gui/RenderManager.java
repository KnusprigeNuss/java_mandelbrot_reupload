package at.tugraz.oop2.gui;


import at.tugraz.oop2.shared.*;
import com.sun.glass.ui.Timer;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

public class RenderManager {
    private final Object renderLock = new Object();
    private final static int threadPoolSize = 8;
    List<Future<?>> renderFutures = new ArrayList<>();
    static SimpleImage[] mandelSubImages = new SimpleImage[threadPoolSize];
    static SimpleImage[] juliaSubImages = new SimpleImage[threadPoolSize];
    public static final SimpleBooleanProperty distributedConcurrencyCheck = new SimpleBooleanProperty(true);
    private long queue = 0;
    public RenderManager() {}

    public void triggerRendering(FractalRenderOptions fractalRenderOptions) {

        if (fractalRenderOptions.getRenderMode() == RenderMode.LOCAL)
        {
            synchronized (renderLock){
                this.queue++;
            }
            //for (Future<?> renderTask : renderTasks) { renderTask.cancel(true); System.out.println("canceled task"); }
            new Thread(() -> {
                try {
                    renderImage(fractalRenderOptions);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                TimerThread.finishedRenderCall();
            }).start();
        }
        else if (fractalRenderOptions.getRenderMode() == RenderMode.DISTRIBUTED)
        {
            new Thread(() -> {
                try {
                    synchronized (distributedConcurrencyCheck) {
                        renderImageDistributed(fractalRenderOptions);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                TimerThread.finishedRenderCall();
            }).start();
        }
    }

    private synchronized void renderImage(FractalRenderOptions fractalRenderOptions) {
        synchronized (renderLock){
            this.queue--;
            if (queue > 0)
            {
                return;
            }
        }
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        renderFutures.clear();
        List<RenderTaskSubImage> renderTasks = new ArrayList<>();
        mandelSubImages = new SimpleImage[threadPoolSize];
        juliaSubImages = new SimpleImage[threadPoolSize];

        int y_size = (fractalRenderOptions.getHeight() / threadPoolSize);
        int filler = fractalRenderOptions.getHeight() - ( (threadPoolSize - 1) * y_size);

        for (int i = 0; i < (threadPoolSize - 1); i++){
            renderTasks.add(new RenderTaskSubImage(fractalRenderOptions, i, y_size, y_size));
            renderFutures.add(executorService.submit(renderTasks.get(renderTasks.size()-1)));
        }

        renderTasks.add(new RenderTaskSubImage(fractalRenderOptions, (threadPoolSize - 1), y_size, filler));
        renderFutures.add(executorService.submit(renderTasks.get(renderTasks.size()-1)));

        boolean renderingValid = false;
        while (true)
        {
            boolean allDone = true;
            for (Future<?> renderFuture : renderFutures) {
                if (!renderFuture.isCancelled() && !renderFuture.isDone()) { allDone = false; }
            }
            synchronized (renderLock){
                if (allDone || this.queue > 0) {
                    if (this.queue > 0)
                    {
                        for (RenderTaskSubImage renderTask : renderTasks)
                        {
                            for (Future<?> renderFuture : renderFutures) {
                                if (!renderFuture.isCancelled()) { renderFuture.cancel(true); }
                            }
                            synchronized (renderTask.deprecatedLock)
                            {
                                renderTask.deprecated = true;
                            }
                        }
                    }
                    else {
                        renderingValid = true;
                    }
                    break;
                }
            }
        }

        executorService.shutdown();
        // System.out.println("Render main thread finished");

        if (renderingValid) {
            try {
                if (fractalRenderOptions.getType() == FractalType.MANDELBROT)
                {
                    SimpleImage renderedImageTest = new SimpleImage(Arrays.asList(mandelSubImages), true);
                    FractalApplication.displayLeftImage(renderedImageTest);
                }
                else /*if (fractalRenderOptions.getType() == FractalType.JULIA)*/{
                    SimpleImage renderedImageTest = new SimpleImage(Arrays.asList(juliaSubImages), true);
                    FractalApplication.displayRightImage(renderedImageTest);
                }
            }
            catch (Exception ignored) {
                FractalApplication.updateRenderMode(RenderMode.LOCAL);
                fractalRenderOptions.setRenderMode(RenderMode.LOCAL);
                triggerRendering(fractalRenderOptions);
            }
        }
        renderFutures.clear();
    }

    private synchronized void renderImageDistributed(FractalRenderOptions fractalRenderOptions) throws Exception {
        synchronized (renderLock){
            this.queue--;
            if (queue > 0)
            {
                return;
            }
        }
        FractalApplication.connectionManager.checkWorkerConnections(false);
        List<WorkerConnection> workerConnections = FractalApplication.connectionManager.getConnectedWorkers();
        if (workerConnections.size() == 0)
        {
            FractalApplication.updateRenderMode(RenderMode.LOCAL);
            fractalRenderOptions.setRenderMode(RenderMode.LOCAL);
            triggerRendering(fractalRenderOptions);
            synchronized (distributedConcurrencyCheck){
                distributedConcurrencyCheck.set(true);
            }
            return;
        }
        // System.out.println("Rendering with " + workerConnections.size() + " workers");
        ExecutorService executorService = Executors.newFixedThreadPool(workerConnections.size());
        renderFutures.clear();

        List<WorkerRenderTaskSubImage> renderTasks = new ArrayList<>();
        int tasksPerWorker = FractalApplication.taskPerWorkerProperty.get();
        tasksPerWorker = Math.min(tasksPerWorker, 100);
        int fractalCount = workerConnections.size() * tasksPerWorker;
        int y_size = (fractalRenderOptions.getHeight() / fractalCount);

        if (y_size == 0)
        {
            return;
        }

        mandelSubImages = new SimpleImage[fractalCount];
        juliaSubImages = new SimpleImage[fractalCount];

        for (int worker_id = 0; worker_id < workerConnections.size(); worker_id++){
            List<Integer> sizes = new ArrayList<>();
            for (int workerSubImageId = 0; workerSubImageId < tasksPerWorker; workerSubImageId++) {
                if ((worker_id == (workerConnections.size() - 1)) && (workerSubImageId == (tasksPerWorker - 1))){
                    sizes.add((fractalRenderOptions.getHeight() - ( (fractalCount - 1) * y_size)));
                }
                else {
                    sizes.add(y_size);
                }
            }
            //System.out.println("Worker " + worker_id + " got " + sizes.size() + "subimages");
            renderTasks.add(new WorkerRenderTaskSubImage(fractalRenderOptions, worker_id, sizes, y_size,
                    workerConnections.get(worker_id)));
            renderFutures.add(executorService.submit(renderTasks.get(renderTasks.size()-1)));
        }

        boolean renderingValid = false;
        while (true)
        {
            boolean allDone = true;
            for (Future<?> renderFuture : renderFutures) {
                if (!renderFuture.isCancelled() && !renderFuture.isDone()) { allDone = false; }
            }
            synchronized (renderLock){
                if (allDone || this.queue > 0) {
                    if (this.queue > 0)
                    {
                        for (WorkerRenderTaskSubImage renderTask : renderTasks)
                        {
                            for (Future<?> renderFuture : renderFutures) {
                                if (!renderFuture.isCancelled()) { renderFuture.cancel(true); }
                            }
                            synchronized (renderTask.deprecatedLock)
                            {
                                renderTask.deprecated = true;
                            }
                        }
                    }
                    else {
                        renderingValid = true;
                    }
                    break;
                }
            }
        }

        executorService.shutdown();
        //System.out.println("Render main thread finished");

        if (renderingValid) {
            try {
                if (fractalRenderOptions.getType() == FractalType.MANDELBROT)
                {
                    SimpleImage renderedImageTest = new SimpleImage(Arrays.asList(mandelSubImages), true);
                    FractalApplication.displayLeftImage(renderedImageTest);
                }
                else /*if (fractalRenderOptions.getType() == FractalType.JULIA)*/{
                    SimpleImage renderedImageTest = new SimpleImage(Arrays.asList(juliaSubImages), true);
                    FractalApplication.displayRightImage(renderedImageTest);
                }
            }
            catch (Exception ignored){
                //System.out.println("Rerendering local");
                renderFutures.clear();
                synchronized (distributedConcurrencyCheck){
                    distributedConcurrencyCheck.set(true);
                }
                FractalApplication.updateRenderMode(RenderMode.LOCAL);
                fractalRenderOptions.setRenderMode(RenderMode.LOCAL);
                triggerRendering(fractalRenderOptions);
            }
        }
    }

    private static class RenderTaskSubImage implements Runnable {
        private final FractalRenderOptions fractalRenderOptions;
        public boolean deprecated = false;
        private final Object deprecatedLock = new Object();
        int y_order;
        int y_size;
        int y_norm_size;
        public RenderTaskSubImage(FractalRenderOptions fractalRenderOptions, int y_order, int y_norm_size, int y_size) {
            this.fractalRenderOptions = fractalRenderOptions;
            this.y_order = y_order;
            this.y_norm_size = y_norm_size;
            this.y_size = y_size;
        }

        @Override
        public void run() {
            // System.out.println("render thread started, y order: " + y_order + " with size: " + y_size);

            // RENDERING
            SimpleImage subImage = new SimpleImage(fractalRenderOptions.getWidth(), y_size);

            if (fractalRenderOptions.getType() == FractalType.MANDELBROT)
            {
                subImage.mandelbrotCalculation((MandelbrotRenderOptions) fractalRenderOptions,
                        y_order * y_norm_size);
            }

            else /*if (fractalRenderOptions.getType() == FractalType.JULIA)*/
            {
                subImage.juliaCalculation((JuliaRenderOptions) fractalRenderOptions,
                        y_order * y_norm_size);
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

            // System.out.println("render thread finished with y order " + y_order);
        }
    }

    private static class WorkerRenderTaskSubImage implements Runnable {
        private final FractalRenderOptions fractalRenderOptions;
        public boolean deprecated = false;
        private final Object deprecatedLock = new Object();
        int y_worker_order;
        int y_size;
        WorkerConnection workerConnection;
        List<Integer> sizes;
        public WorkerRenderTaskSubImage(FractalRenderOptions fractalRenderOptions, int y_worker_order,
                                        List<Integer> sizes, int y_size, WorkerConnection workerConnection) {
            this.fractalRenderOptions = fractalRenderOptions;
            this.y_worker_order = y_worker_order;
            this.y_size = y_size;
            this.workerConnection = workerConnection;
            this.sizes = sizes;
        }

        @Override
        public void run() {
            // System.out.println("render thread started, y order: " + y_order + " with size: " + y_size);
            List<SimpleImage> subImages;
            try {
                subImages = workerConnection.renderImage(fractalRenderOptions, y_worker_order, sizes,
                        y_size);
                //System.out.println("y_worker_order: " + y_worker_order + " at con " + workerConnection);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            workerConnection.setRenderingFinished(false);

            // DISCARD IMAGE IF DEPRECATED
            synchronized (deprecatedLock)
            {
                if (!deprecated){
                    if (fractalRenderOptions.getType() == FractalType.MANDELBROT)
                    {
                        for (int i = 0; i < subImages.size(); i++){
                            mandelSubImages[(y_worker_order * subImages.size()) + i] = subImages.get(i);
                        }
                    }
                    else /*if (fractalRenderOptions.getType() == FractalType.JULIA)*/
                    {
                        for (int i = 0; i < subImages.size(); i++) {
                            juliaSubImages[(y_worker_order * subImages.size()) + i] = subImages.get(i);
                        }
                    }
                }
            }

            // System.out.println("render thread finished with y order " + y_order);
        }
    }
}
