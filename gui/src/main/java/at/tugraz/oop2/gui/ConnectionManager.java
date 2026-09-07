package at.tugraz.oop2.gui;

import at.tugraz.oop2.shared.RenderMode;
import at.tugraz.oop2.shared.WorkerConnection;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ConnectionManager {
    private final StringProperty connectionStringProperty = new SimpleStringProperty();
    private final List<WorkerConnection> workers = new ArrayList<>();
    private final List<WorkerConnection> connectedWorkers = new ArrayList<>();

    public SimpleIntegerProperty numberOfWorkers = new SimpleIntegerProperty(0);
    public SimpleIntegerProperty numberOfConnectedWorkers = new SimpleIntegerProperty(0);

    public ConnectionManager() {
        this.connectionStringProperty.set("");
        this.numberOfWorkers.set(0);
    }

    public void addConnectionStringElement(String toAdd) throws InterruptedException {
        synchronized (connectionStringProperty) {
            this.connectionStringProperty.set(this.connectionStringProperty.get() + "," + toAdd);
        }
        this.numberOfWorkers.set(getConnectionStringElements().size());
        WorkerConnection workerConnection = new WorkerConnection(toAdd);
        this.workers.add(workerConnection);
        Thread workerThread = new Thread(workerConnection);
        workerThread.setDaemon(true);
        workerThread.start();
    }

    public StringProperty getConnectionStringProperty(){
        synchronized (this.connectionStringProperty) {
            return this.connectionStringProperty;
        }
    }

    public void setConnectionString(String newConnectionString) throws IOException, InterruptedException {
        synchronized (this.connectionStringProperty){
            this.connectionStringProperty.set(newConnectionString);
        }
        this.numberOfWorkers.set(getConnectionStringElements().size());
        creatingWorkerConnections();
    }

    public List<String> getConnectionStringElements(){
        List<String> elements = new ArrayList<>();
        for (String s : getConnectionStringProperty().get().split(",")){
            if (!s.equals("")){
                elements.add(s);
            }
        }
        return elements;
    }

    public List<WorkerConnection> getConnectedWorkers(){
        return this.connectedWorkers;
    }

    // Call this method initially / after setting / updating the connection string accordingly to
    // connect to the workers
    public void creatingWorkerConnections() throws IOException, InterruptedException {
        System.out.println("WRONG");
        List<String> workerStrings = getConnectionStringElements();
        List<WorkerConnection> newWorkers = new ArrayList<>();
        for (String workerString : workerStrings)
        {
            WorkerConnection workerConnection = new WorkerConnection(workerString);
            newWorkers.add(workerConnection);
            Thread workerThread = new Thread(workerConnection);
            workerThread.setDaemon(true);
            workerThread.start();
        }
        this.workers.clear();
        this.workers.addAll(newWorkers);
        this.numberOfWorkers.set(this.workers.size());
        Thread.sleep(1000);
        checkWorkerConnections(true);
    }

    public int getNumberOfWorkers() {
        return numberOfWorkers.get();
    }

    public int getNumberOfConnectedWorkers() {
        return this.numberOfConnectedWorkers.get();
    }
    @SuppressWarnings("All")
    public synchronized void checkWorkerConnections(boolean deep) throws InterruptedException {
        List<WorkerConnection> validWorkerConnections = new ArrayList<WorkerConnection>();
        synchronized (this.workers){
            for (int i = 0; i < this.workers.size(); i++)
            {
                if (deep && this.workers.get(i).timedOut){
                    this.workers.set(i, new WorkerConnection(this.workers.get(i).getConnectionString()));
                    Thread workerThread = new Thread(this.workers.get(i));
                    workerThread.setDaemon(true);
                    workerThread.start();
                }
            }
            if (deep){
                Thread.sleep(1000);
            }
            for (int i = 0; i < this.workers.size(); i++){
                if (this.workers.get(i).getConnection() != null && this.workers.get(i).getConnection().isConnected())
                {
                    validWorkerConnections.add(this.workers.get(i));
                }
            }
        }
        synchronized (this.connectedWorkers){
            this.connectedWorkers.clear();
            this.connectedWorkers.addAll(validWorkerConnections);
            this.numberOfConnectedWorkers.set(this.connectedWorkers.size());
        }
        Thread.sleep(100);
        if (connectedWorkers.size() > 0){
            FractalApplication.updateRenderMode(RenderMode.DISTRIBUTED);
        }
    }

    public void dropConnections() throws IOException {
        for (WorkerConnection worker : workers){
            worker.closeConnection();
        }
        numberOfWorkers.set(0);
        numberOfConnectedWorkers.set(0);
        connectedWorkers.clear();
        workers.clear();
    }

    public void dropConnection(String connectionString) throws IOException, InterruptedException {
        boolean found = false;
        for (WorkerConnection worker : this.workers){
            if (worker.getConnectionString().equals(connectionString)){
                //System.out.println("removing worker");
                worker.closeConnection();
                workers.remove(worker);
                found = true;
                break;
            }
        }
        if (found) {
            //System.out.println("removing connection from connectionString");
            this.connectionStringProperty.set(this.connectionStringProperty.get().replaceFirst((connectionString + ","), ""));
            this.numberOfWorkers.set(getConnectionStringElements().size());
        }
    }

}
