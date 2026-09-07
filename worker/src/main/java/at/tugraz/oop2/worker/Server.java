package at.tugraz.oop2.worker;
import at.tugraz.oop2.shared.FractalLogger;

import java.io.IOException;
import java.net.ServerSocket;

public class Server {

    @SuppressWarnings("All")
    public static void main(String[] args) throws IOException, InterruptedException {

        int port = parseServer(args[0]);
        int connectionId = 0;
        ServerSocket serverSocket = new ServerSocket(port);
        FractalLogger.logStartWorker(port);

        Thread listening_thread;
        Connection latest_connection = null;

        while(true)
        {
            if (latest_connection == null || !latest_connection.free)
            {
                latest_connection = new Connection(serverSocket, connectionId);
                connectionId++;
                listening_thread = new Thread(latest_connection);
                listening_thread.start();
            }
            Thread.sleep(1000);
        }
    }

    public static boolean numericCheck(String teststr) {
        try {
            Double.parseDouble(teststr);
        } catch (NumberFormatException a) {
            return false;
        }
        return true;
    }

    @SuppressWarnings("All")
    public static int parseServer(String arg) {
        String port_cmp = "port";

        // check for -- again
        StringBuilder compare_components = new StringBuilder();
        compare_components.append(arg.charAt(0));
        compare_components.append(arg.charAt(1));
        String compare_string = compare_components.toString();
        String param_start = "--";
        if (compare_string.equals(param_start)) {
            String only_input_name = arg.substring(2);
            String[] attribute_and_value = only_input_name.split("=");
            int count = 0;
            for (String every : attribute_and_value){
                count = count + 1;
            }
            // check for more =
            if (count != 2){
                return 8010;
            }

            String case_sens_attr = attribute_and_value[0];
            // case insensitive
            String attribute = case_sens_attr.toLowerCase();
            String value = attribute_and_value[1];

            // is numeric checks
            if (attribute.equals(port_cmp)) {
                if (numericCheck(value)) {
                    return Integer.parseInt(value);
                }
                else {
                    return 8010;
                }
            }
        }
        return 8010;
    }
}
